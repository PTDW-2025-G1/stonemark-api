package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.shared.utils.VectorUtils;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaSimilarityEngine {

    private final MarkEvidenceRepository evidenceRepository;
    private final MeterRegistry meterRegistry;

    /**
     * Compute suggestions in-JVM for the given processing. This method loads evidence
     * embeddings, computes cosine similarity, records filtered metrics and returns
     * the ordered list of suggestions.
     */
    public List<MarkSuggestion> computeSuggestions(
            MarkEvidenceProcessing processing,
            int k,
            double minSimilarity,
            boolean useRankWeighting
    ) {

        List<MarkEvidence> rows = evidenceRepository.findAllByEmbeddingIsNotNull();
        if (rows == null || rows.isEmpty()) {
            try { meterRegistry.counter("processing.suggestions.filtered.count", "submission", processing.getSubmission().getId().toString()).increment(0); } catch (Exception ignored) {}
            return List.of();
        }

        long considered = rows.stream().filter(r -> r.getEmbedding() != null && r.getEmbedding().length > 0 && r.getOccurrence() != null && r.getOccurrence().getMark() != null && r.getOccurrence().getMark().getId() != null).count();
        List<Map.Entry<MarkEvidence, Double>> scored = rows.stream()
                .filter(r -> r.getEmbedding() != null && r.getEmbedding().length > 0 && r.getOccurrence() != null && r.getOccurrence().getMark() != null && r.getOccurrence().getMark().getId() != null)
                .map(r -> Map.entry(r, VectorUtils.cosineSimilarity(processing.getEmbedding(), r.getEmbedding())))
                .toList();

        long passing = scored.stream().filter(e -> e.getValue() != null && e.getValue() >= minSimilarity).count();
        long filtered = Math.max(0L, considered - passing);
        try { meterRegistry.counter("processing.suggestions.filtered.count", "submission", processing.getSubmission().getId().toString()).increment(filtered); } catch (Exception ignored) {}

        List<Map.Entry<MarkEvidence, Double>> filteredScored = scored.stream()
                .filter(e -> e.getValue() != null && e.getValue() >= minSimilarity)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(k)
                .toList();

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Double> weightSums = new HashMap<>();
        Map<Long, pt.estga.mark.entities.Mark> marksById = new HashMap<>();

        Set<UUID> seen = new HashSet<>();
        for (int idx = 0; idx < filteredScored.size(); idx++) {
            var entry = filteredScored.get(idx);
            MarkEvidence ev = entry.getKey();
            double similarity = entry.getValue();
            UUID id = ev.getId();
            if (!seen.add(id)) continue;
            pt.estga.mark.entities.Mark mark = ev.getOccurrence().getMark();
            if (mark == null || mark.getId() == null) continue;
            Long markId = mark.getId();

            double weight = useRankWeighting ? 1.0 / (1 + idx) : 1.0;
            double weighted = similarity * weight;

            scores.merge(markId, weighted, Double::sum);
            weightSums.merge(markId, weight, Double::sum);
            marksById.putIfAbsent(markId, mark);
        }

        if (scores.isEmpty()) return List.of();

        List<MarkSuggestion> result = scores.entrySet().stream()
                .filter(entry -> {
                    Long markId = entry.getKey();
                    Double weightSum = weightSums.get(markId);
                    return weightSum != null && weightSum != 0.0 && marksById.containsKey(markId);
                })
                .map(entry -> {
                    Long markId = entry.getKey();
                    double totalScore = entry.getValue();
                    double weightSum = weightSums.get(markId);
                    double confidence = totalScore / weightSum;
                    return MarkSuggestion.builder()
                            .processing(processing)
                            .mark(marksById.get(markId))
                            .confidence(confidence)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(5)
                .toList();

        return result;
    }
}
