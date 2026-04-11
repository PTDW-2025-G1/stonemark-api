package pt.estga.processing.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.shared.utils.VectorUtils;

import java.util.*;

@Component
@Slf4j
public class JavaSimilarityEngine {

    /**
     * Compute suggestions in-JVM given preloaded evidence rows.
     * Returns an ordered, limited list of MarkSuggestion instances.
     */
    public List<MarkSuggestion> computeSuggestions(
            MarkEvidenceProcessing processing,
            List<MarkEvidence> rows,
            int k,
            double minSimilarity,
            boolean useRankWeighting
    ) {

        if (rows == null || rows.isEmpty()) return List.of();

        // Score each evidence by cosine similarity and keep top-k
        List<Map.Entry<MarkEvidence, Double>> scored = rows.stream()
                .filter(r -> r.getEmbedding() != null && r.getEmbedding().length > 0 && r.getOccurrence() != null)
                .map(r -> Map.entry(r, VectorUtils.cosineSimilarity(processing.getEmbedding(), r.getEmbedding())))
                .filter(e -> e.getValue() != null && e.getValue() >= minSimilarity)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(k)
                .toList();

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Double> weightSums = new HashMap<>();
        Map<Long, Mark> marksById = new HashMap<>();

        Set<UUID> seen = new HashSet<>();
        for (int idx = 0; idx < scored.size(); idx++) {
            var entry = scored.get(idx);
            MarkEvidence ev = entry.getKey();
            double similarity = entry.getValue();
            UUID id = ev.getId();
            if (!seen.add(id)) continue;
            Mark mark = ev.getOccurrence().getMark();
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
