package pt.estga.processing.services.similarity;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.models.SanitizationResult;
import pt.estga.processing.services.similarity.aggregation.MarkAggregator;
import pt.estga.processing.services.similarity.suggestionbuilder.SuggestionBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    private final MeterRegistry meterRegistry;
    private final EmbeddingPreprocessor embeddingPreprocessor;
    private final CandidateFetcher candidateFetcher;
    private final CandidateSanitizer candidateSanitizer;
    private final MarkAggregator markAggregator;
    private final ParityChecker parityChecker;
    private final ProcessingProperties properties;
    private final SuggestionBuilder suggestionBuilder;
    private boolean parityEnabledLocal;
    private int maxKLocal;
    private double maxDistanceLocal;

    void maybeRunParityCheck() {
        if (!parityEnabledLocal) return;
        try {
            parityChecker.maybeRun();
        } catch (Exception e) {
            log.error("Similarity parity check failed (non-fatal): {}", e.getMessage());
        }
    }

    @PostConstruct
    void initLocalPropertiesAndMaybeRunParity() {
        this.parityEnabledLocal = properties.similarity().parityEnabled();
        this.maxKLocal = properties.similarity().maxK();
        this.maxDistanceLocal = properties.similarity().maxDistance();

        maybeRunParityCheck();
    }

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {

        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) {
            log.warn("Processing {} (submission={}) has no embedding, skipping similarity",
                    processing == null ? "null" : processing.getId(),
                    processing.getSubmissionId() == null ? "null" : String.valueOf(processing.getSubmissionId()));
            return List.of();
        }

        Optional<String> maybeVector = embeddingPreprocessor.toDbVector(processing);
        if (maybeVector.isEmpty()) return List.of();
        String vector = maybeVector.get();

        long start = System.nanoTime();
        List<MarkSuggestion> result;

        int safeK = Math.max(1, Math.min(k, maxKLocal));
        if (k > maxKLocal) {
            try { meterRegistry.counter("processing.suggestions.k_clamped.count", "engine", "db").increment(); } catch (Exception ignored) {}
            log.warn("Requested k={} exceeds maxK={}, clamping to {}", k, maxKLocal, safeK);
        }
        double maxDistance = maxDistanceLocal;

        List<MarkEvidenceDistanceDto> hits = candidateFetcher.fetchCandidates(vector, safeK, maxDistance);
        SanitizationResult sanitized = candidateSanitizer.sanitize(hits);
        if (sanitized.candidates().isEmpty()) return List.of();

        List<EvidenceMarkDto> rows = candidateFetcher.fetchMarksByEvidenceIds(new ArrayList<>(sanitized.idSet()));
        Map<UUID, List<Long>> markIdsByEvidenceId = new LinkedHashMap<>();
        for (EvidenceMarkDto r : rows) {
            if (r == null) continue;
            UUID id = r.evidenceId();
            Long markId = r.markId();
            if (id == null || markId == null) continue;
            markIdsByEvidenceId.computeIfAbsent(id, _ -> new ArrayList<>()).add(markId);
        }

        Set<UUID> mappedIds = new HashSet<>();
        for (EvidenceMarkDto r : rows) {
            if (r == null) continue;
            if (r.evidenceId() != null) mappedIds.add(r.evidenceId());
        }
        int missingMarkMappings = Math.max(0, sanitized.idSet().size() - mappedIds.size());

        AggregationResult aggregation = markAggregator.aggregate(sanitized.candidates(), markIdsByEvidenceId, k, missingMarkMappings);

        try { meterRegistry.counter("processing.suggestions.invalid.similarity.count", "engine", "db").increment(sanitized.invalidSimilarityCount()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.similarity.out_of_range.count", "engine", "db").increment(sanitized.outOfRangeCount()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.duplicates.count", "engine", "db").increment(aggregation.duplicates()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.per_mark_contributions.count", "engine", "db").increment(aggregation.perMarkContributions()); } catch (Exception ignored) {}
        try { meterRegistry.counter("processing.suggestions.per_mark_decay_applied.count", "engine", "db").increment(aggregation.perMarkDecayApplied()); } catch (Exception ignored) {}

        result = suggestionBuilder.buildSuggestions(aggregation.topScores(), processing);

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
                    processing.getSubmissionId() == null ? "null" : String.valueOf(processing.getSubmissionId()),
                    result.size(),
                    topConfidence);
        }

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
