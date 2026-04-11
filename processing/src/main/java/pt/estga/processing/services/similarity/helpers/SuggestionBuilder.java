package pt.estga.processing.services.similarity.helpers;

import org.springframework.stereotype.Service;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;

import java.util.*;

@Service
public class SuggestionBuilder {

    /**
     * Build MarkSuggestion entities from aggregation outputs.
     * This is a small pure transformer: it does not access DB or emit metrics.
     */
    public List<MarkSuggestion> buildSuggestions(
            Map<Long, Double> scores,
            Map<Long, Double> weightSums,
            Map<Long, Mark> marksById,
            MarkEvidenceProcessing processing,
            int k
    ) {
        if (scores == null || scores.isEmpty()) return List.of();

        return scores.entrySet().stream()
                .filter(entry -> {
                    Long markId = entry.getKey();
                    Double weightSum = weightSums.get(markId);
                    if (weightSum == null || weightSum == 0.0) return false;
                    return marksById.containsKey(markId);
                })
                .map(entry -> {
                    Long markId = entry.getKey();
                    double totalScore = entry.getValue();
                    double weightSum = weightSums.get(markId);
                    double confidence = totalScore / weightSum;
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
}
