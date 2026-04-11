package pt.estga.processing.services.similarity.helpers.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.MarkScore;
import pt.estga.processing.models.AggregationResult;

import java.util.*;

@Component
public class AggregationResultBuilder {

    public AggregationResult build(ScoreCalculator.AggregationState state, Map<Long, Mark> marksById, int k) {
        List<MarkScore> topScores = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : state.scores().entrySet()) {
            Long markId = entry.getKey();
            double totalScore = entry.getValue();
            Double weight = state.weightSums().get(markId);
            if (weight == null || weight == 0.0) {
                // data issue: missing weight for markId
                continue;
            }
            Mark m = marksById.get(markId);
            if (m == null) {
                continue;
            }
            double confidence = totalScore / weight;
            double quantized = Math.round(confidence * 1_000_000d) / 1_000_000d;
            topScores.add(new MarkScore(markId, m, quantized));
        }

        // Sort deterministically: confidence desc, markId asc
        topScores.sort((a, b) -> {
            int cmp = Double.compare(b.confidence(), a.confidence());
            if (cmp != 0) return cmp;
            if (a.markId() == null && b.markId() == null) return 0;
            if (a.markId() == null) return 1;
            if (b.markId() == null) return -1;
            return a.markId().compareTo(b.markId());
        });

        List<MarkScore> limited = (k > 0 && topScores.size() > k) ? topScores.subList(0, k) : topScores;

        return new AggregationResult(limited, state.duplicates(), state.perMarkContributions(), state.perMarkDecayApplied());
    }
}
