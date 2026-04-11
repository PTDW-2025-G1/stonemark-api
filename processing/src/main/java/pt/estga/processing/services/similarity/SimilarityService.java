package pt.estga.processing.services.similarity;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.config.policies.SimilarityPolicy;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import java.util.*;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.SanitizationResult;
import pt.estga.processing.services.similarity.helpers.*;
import pt.estga.processing.services.similarity.helpers.aggregation.MarkAggregator;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    /**
     * Computes similarity-based Mark suggestions for a processed submission.
     * <p>
     * Uses DB-backed vector search for candidate retrieval and JVM-side aggregation
     * to compute per-mark confidence scores.
     * <p>
     * Returns up to k ranked MarkSuggestion results.
     */

    private final MeterRegistry meterRegistry;
    private final EmbeddingPreprocessor embeddingPreprocessor;
    private final CandidateFetcher candidateFetcher;
    private final CandidateSanitizer candidateSanitizer;
    private final MarkAggregator markAggregator;
    private final ParityChecker parityChecker;
    private final SimilarityPolicy similarityPolicy;
    private final SuggestionBuilder suggestionBuilder;
    // Configuration values
    private boolean parityEnabledLocal;
    private int maxKLocal;
    private double maxDistanceLocal;

    void maybeRunParityCheck() {
        if (!parityEnabledLocal) return;
        try {
            parityChecker.maybeRun();
        } catch (Exception e) {
            // Parity check is diagnostic only. Log the failure and continue startup so
            // parity mismatches do not become deployment blockers.
            log.error("Similarity parity check failed (non-fatal): {}", e.getMessage());
        }
    }

    @PostConstruct
    void initLocalPropertiesAndMaybeRunParity() {
        // Initialize cached properties
        this.parityEnabledLocal = similarityPolicy.isParityEnabled();
        this.maxKLocal = similarityPolicy.getMaxK();
        this.maxDistanceLocal = similarityPolicy.getMaxDistance();

        // Run parity check if enabled
        maybeRunParityCheck();
    }

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
        int safeK = Math.max(1, Math.min(k, maxKLocal));
        if (k > maxKLocal) {
            try { meterRegistry.counter("processing.suggestions.k_clamped.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Requested k={} exceeds maxK={}, clamping to {}", k, maxKLocal, safeK);
        }
        double maxDistance = maxDistanceLocal;

        // Fetch DB candidates (DB boundary)
        List<MarkEvidenceDistanceProjection> hits = candidateFetcher.fetchCandidates(vector, safeK, maxDistance);
        // Sanitize DB results (trust boundary)
        SanitizationResult sanitized = candidateSanitizer.sanitize(hits);
        if (sanitized.candidates().isEmpty()) return List.of();

        // Fetch mark mappings for the candidate evidence ids via DB boundary
        List<EvidenceMarkProjection> rows = candidateFetcher.fetchMarksByEvidenceIds(new ArrayList<>(sanitized.idSet()));
        Map<UUID, Mark> markByEvidenceId = new LinkedHashMap<>();
        for (EvidenceMarkProjection r : rows) {
            if (r == null) continue;
            UUID id = r.getId();
            Mark m = r.getMark();
            if (id == null || m == null) continue;
            Mark existing = markByEvidenceId.get(id);
            if (existing == null) {
                markByEvidenceId.put(id, m);
            } else if (!Objects.equals(existing.getId(), m.getId())) {
                // Data inconsistency: same evidence id mapped to different marks — keep first mapping
                log.warn("Duplicate mark mapping encountered for evidence id {}: keeping mark id {} over {}", id, existing.getId(), m.getId());
            }
        }

        // Aggregate contributions (core business logic)
        AggregationResult aggregation = markAggregator.aggregate(sanitized.candidates(), markByEvidenceId, k);

        // Emit aggregated metrics collected during sanitization/aggregation
        try { meterRegistry.counter("processing.suggestions.invalid.similarity.count", "engine", "db").increment(sanitized.invalidSimilarityCount()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.similarity.out_of_range.count", "engine", "db").increment(sanitized.outOfRangeCount()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.duplicates.count", "engine", "db").increment(aggregation.duplicates()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.per_mark_contributions.count", "engine", "db").increment(aggregation.perMarkContributions()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.per_mark_decay_applied.count", "engine", "db").increment(aggregation.perMarkDecayApplied()); } catch (Exception ignored) {}

        // Build final suggestions from aggregation results (pure transformation)
        result = suggestionBuilder.buildSuggestions(aggregation.topScores(), processing);


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
