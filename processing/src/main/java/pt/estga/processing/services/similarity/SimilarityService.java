package pt.estga.processing.services.similarity;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.SanitizationResult;
import pt.estga.processing.services.similarity.helpers.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    /**
     * Similarity service (DB-backed candidate retrieval + JVM-side scoring).
     * <p>
     * Architecture note (explicit): this service treats the database as the
     * candidate retrieval layer (approximate nearest neighbours via pgvector)
     * and applies a JVM-side scoring/aggregation layer to produce per-mark
     * suggestions with confidence. Invariants and contract:
     *  - Embeddings MUST be L2-normalized (unit length) at ingestion time. The
     *    service enforces normalization for newly produced embeddings, but
     *    historical data must also be normalized to guarantee parity.
     *  - The DB projection MUST return a similarity value computed as
     *    1.0 - (me.embedding <#> CAST(:vector AS vector)) (i.e. 1 - cosine_distance).
     *    If the DB operator, index, or stored vectors change (e.g. switch to
     *    L2 or inner-product) this contract must be updated accordingly.
     * <p>
     * The scoring layer performs defensive validation of DB-provided similarities
     * (null/NaN/non-finite/out-of-range) and clamps values to [0.0, 1.0] before
     * using them in weighting to avoid propagation of corrupted data.
     */

    private final MeterRegistry meterRegistry;
    private final EmbeddingPreprocessor embeddingPreprocessor;
    private final CandidateFetcher candidateFetcher;
    private final CandidateSanitizer candidateSanitizer;
    private final MarkAggregator markAggregator;
    private final ParityChecker parityChecker;
    private final ProcessingProperties properties;

    @PostConstruct
    void maybeRunParityCheck() {
        if (!properties.getSimilarity().getParityCheck().isEnabled()) return;
        try {
            parityChecker.maybeRun();
        } catch (Exception e) {
            log.error("Similarity parity check failed: {}", e.getMessage());
            if (!properties.getSimilarity().getParityCheck().isAsync()) throw new IllegalStateException("Similarity parity check failed: " + e.getMessage(), e);
        }
    }

    // Parity check delegated to ParityChecker

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {

        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) {
            log.warn("Processing {} (submission={}) has no embedding, skipping similarity",
                    processing == null ? "null" : processing.getId(),
                    processing == null || processing.getSubmission() == null ? "null" : processing.getSubmission().getId());
            return List.of();
        }

        // Validate and normalize embedding (returns vector literal)
        Optional<String> maybeVector = embeddingPreprocessor.toDbVector(processing);
        if (maybeVector.isEmpty()) return List.of();
        String vector = maybeVector.get();

        long start = System.nanoTime();
        List<MarkSuggestion> result;

        // Always use DB-backed similarity. Java in-process engine has been removed to avoid divergence
        // and maintain a single source of truth for similarity ranking.
        // Clamp k to a safe maximum to avoid extremely large IN(...) queries and planner issues.
        int safeK = Math.max(1, Math.min(k, properties.getSimilarity().getMaxK()));
        if (k > properties.getSimilarity().getMaxK()) {
            try { meterRegistry.counter("processing.suggestions.k_clamped.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Requested k={} exceeds maxK={}, clamping to {}", k, properties.getSimilarity().getMaxK(), safeK);
        }
        double maxDistance = Math.max(0.0, 1.0 - properties.getSimilarity().getMinScore());

        // Fetch DB candidates (DB boundary)
        List<MarkEvidenceDistanceProjection> hits = candidateFetcher.fetchCandidates(vector, safeK, maxDistance);
        // Sanitize DB results (trust boundary)
        SanitizationResult sanitized = candidateSanitizer.sanitize(hits);
        if (sanitized.candidates().isEmpty()) return List.of();

        // Fetch mark mappings for the candidate evidence ids via DB boundary
        List<EvidenceMarkProjection> rows = candidateFetcher.fetchMarksByEvidenceIds(new ArrayList<>(sanitized.idSet()));
        Map<UUID, Mark> markByEvidenceId = rows.stream().collect(Collectors.toMap(
                EvidenceMarkProjection::getId,
                EvidenceMarkProjection::getMark,
                (a, b) -> a
        ));

        // Aggregate contributions (core business logic)
        AggregationResult aggregation = markAggregator.aggregate(sanitized.candidates(), markByEvidenceId);

        // Emit metrics collected during sanitization/aggregation in a controlled
        // place (orchestration) rather than scattering counters through logic.
        try { meterRegistry.counter("processing.suggestions.invalid.similarity.count", "engine", "db").increment(sanitized.invalidSimilarityCount()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.similarity.out_of_range.count", "engine", "db").increment(sanitized.outOfRangeCount()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.duplicates.count", "engine", "db").increment(aggregation.duplicates()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.per_mark_contributions.count", "engine", "db").increment(aggregation.perMarkContributions()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.per_mark_decay_applied.count", "engine", "db").increment(aggregation.perMarkDecayApplied()); } catch (Exception ignored) {}

        // Record final suggestions count (build from aggregation result)
        Map<Long, Double> scores = aggregation.scores();
        Map<Long, Double> weightSums = aggregation.weightSums();
        Map<Long, Mark> marksById = aggregation.marksById();

        if (scores.isEmpty()) {
            result = List.of();
        } else {
            result = scores.entrySet().stream()
            // pre-filter entries with valid weight sums to avoid nulls in the mapping stage
            .filter(entry -> {
                Long markId = entry.getKey();
                Double weightSum = weightSums.get(markId);
                if (weightSum == null || weightSum == 0.0) {
                    log.warn("Invariant violation: weightSum missing for mark id {}", markId);
                    return false;
                }
                // ensure we have the actual Mark entity for the id
                if (!marksById.containsKey(markId)) {
                    log.warn("Missing Mark entity for id {} while aggregating scores", markId);
                    return false;
                }
                return true;
            })
            .map(entry -> {
                Long markId = entry.getKey();
                double totalScore = entry.getValue();
                double weightSum = weightSums.get(markId);

                // Normalize by the total weight to produce a weighted average confidence.
                double confidence = totalScore / weightSum;
                // Quantize confidence to 1e-6 to improve determinism across runs and reduce
                // floating-point jitter when ranking / comparing results.
                double quantized = Math.round(confidence * 1_000_000d) / 1_000_000d;

                return MarkSuggestion.builder()
                        .processing(processing)
                        .mark(marksById.get(markId))
                        .confidence(quantized)
                        .build();
            })
            .sorted((a, b) -> {
                int cmp = Double.compare(b.getConfidence(), a.getConfidence());
                if (cmp != 0) return cmp;
                Long aId = a.getMark() == null ? null : a.getMark().getId();
                Long bId = b.getMark() == null ? null : b.getMark().getId();
                if (aId == null && bId == null) return 0;
                if (aId == null) return 1;
                if (bId == null) return -1;
                return aId.compareTo(bId);
            })
            .limit(k)
            .toList();
        }


        // filtered metric for DB branch was already recorded as filteredLocal inside the branch when applicable

        try {
            meterRegistry.summary("processing.suggestions.count", "engine", "db", "result", result.isEmpty() ? "empty" : "has")
                    .record(result.size());
        } catch (Exception e) {
            log.debug("Failed to record suggestions metric for processing {}: {}", processing.getId(), e.getMessage());
        }

        try { meterRegistry.counter("processing.suggestions.final_suggestions.count", "engine", "db").increment(result.size()); } catch (Exception ignored) {}

        if (log.isDebugEnabled()) {
            Double topConfidence = result.stream().findFirst().map(MarkSuggestion::getConfidence).orElse(null);
            log.debug("Processing {} (submission={}) → {} suggestions (top confidence={})",
                    processing.getId(),
                    processing.getSubmission() == null ? "null" : processing.getSubmission().getId(),
                    result.size(),
                    topConfidence);
        }

        // Record similarity computation time
        try {
            long elapsedNanos = System.nanoTime() - start;
            meterRegistry.timer("processing.similarity.time", "engine", "db", "result", result.isEmpty() ? "empty" : "has")
                    .record(elapsedNanos, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            log.debug("Failed to record similarity timer for processing {}: {}", processing.getId(), e.getMessage());
        }

        return result;
    }
}
