package pt.estga.processing.services.similarity;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.commoncore.utils.VectorUtils;
import pt.estga.mark.api.MarkEvidenceQueryService;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    private final MeterRegistry meterRegistry;
    private final MarkEvidenceQueryService markEvidenceQueryService;
    private final ProcessingProperties properties;

    public List<MarkSuggestion> findSimilar(MarkEvidenceProcessing processing, int k) {
        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) {
            log.warn("Processing {} has no embedding, skipping similarity", processingId(processing));
            return List.of();
        }

        float[] embedding = processing.getEmbedding();

        double norm = VectorUtils.l2Norm(embedding);
        if (Double.isNaN(norm) || Math.abs(norm - 1.0) > 1e-3) {
            log.warn("Processing {} embedding has unexpected norm {:.4f} — continuing", processing.getId(), norm);
        }

        String vector = VectorUtils.toVectorLiteral(embedding);

        long start = System.nanoTime();
        int safeK = Math.max(1, Math.min(k, properties.similarity().maxK()));
        double maxDistance = properties.similarity().maxDistance();

        List<MarkEvidenceDistanceDto> hits = markEvidenceQueryService.findTopKSimilar(vector, safeK, maxDistance);
        if (hits == null || hits.isEmpty()) return List.of();

        List<MarkEvidenceDistanceDto> valid = filterValid(hits);
        if (valid.isEmpty()) return List.of();

        List<UUID> evidenceIds = valid.stream().map(MarkEvidenceDistanceDto::id).distinct().toList();
        List<EvidenceMarkDto> rows = markEvidenceQueryService.findMarksByEvidenceIds(evidenceIds);

        Map<UUID, List<Long>> markIdsByEvidenceId = new LinkedHashMap<>();
        for (EvidenceMarkDto r : rows) {
            if (r == null || r.evidenceId() == null || r.markId() == null) continue;
            markIdsByEvidenceId.computeIfAbsent(r.evidenceId(), _ -> new ArrayList<>()).add(r.markId());
        }

        Map<Long, Double> markScores = new LinkedHashMap<>();
        for (MarkEvidenceDistanceDto hit : valid) {
            List<Long> markIds = markIdsByEvidenceId.getOrDefault(hit.id(), List.of());
            for (Long markId : markIds) {
                markScores.merge(markId, hit.similarity(), Double::sum);
            }
        }

        List<MarkSuggestion> result = markScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(safeK)
                .map(e -> MarkSuggestion.builder()
                        .processing(processing)
                        .markId(e.getKey())
                        .confidence(Math.min(1.0, Math.max(0.0, Math.round(e.getValue() * 1_000_000d) / 1_000_000d)))
                        .build())
                .toList();

        try {
            meterRegistry.summary("processing.suggestions.count", "result", result.isEmpty() ? "empty" : "has")
                    .record(result.size());
            long elapsedNanos = System.nanoTime() - start;
            meterRegistry.timer("processing.similarity.time", "result", result.isEmpty() ? "empty" : "has")
                    .record(elapsedNanos, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            log.debug("Failed to record similarity metrics for processing {}: {}", processing.getId(), e.getMessage());
        }

        if (log.isDebugEnabled()) {
            Double topConfidence = result.stream().findFirst().map(MarkSuggestion::getConfidence).orElse(null);
            log.debug("Processing {} → {} suggestions (top confidence={})", processing.getId(), result.size(), topConfidence);
        }

        return result;
    }

    private List<MarkEvidenceDistanceDto> filterValid(List<MarkEvidenceDistanceDto> hits) {
        return hits.stream()
                .filter(h -> h != null && h.similarity() != null
                        && Double.isFinite(h.similarity())
                        && !Double.isNaN(h.similarity()))
                .toList();
    }

    private static String processingId(MarkEvidenceProcessing p) {
        return p != null && p.getId() != null ? p.getId().toString() : "null";
    }
}
