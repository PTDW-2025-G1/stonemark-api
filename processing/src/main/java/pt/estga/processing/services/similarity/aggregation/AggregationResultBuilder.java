package pt.estga.processing.services.similarity.aggregation;

import org.springframework.stereotype.Component;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.MarkScore;
import pt.estga.processing.models.AggregationResult;

import java.util.*;

@Component
public class AggregationResultBuilder {

    public AggregationResult build(AggregationState state, Map<Long, Mark> marksById, int k, int missingMarkMappings) {
        List<MarkScore> topScores = new ArrayList<>();
        // Use confidences computed by ScoreCalculator as the single source of truth
        // for per-mark confidence values.
        for (Map.Entry<Long, Double> entry : state.confidences().entrySet()) {
            Long markId = entry.getKey();
            Double confidenceRaw = entry.getValue();
            if (confidenceRaw == null) continue;
            Mark m = marksById.get(markId);
            if (m == null) continue;
            double quantized = Math.round(confidenceRaw * 1_000_000d) / 1_000_000d;
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

        // Provide raw score maps for debugging; expose as unmodifiable copies to callers
        Map<Long, Double> rawScoresCopy = Collections.unmodifiableMap(new TreeMap<>(state.scores()));
        Map<Long, Double> weightSumsCopy = Collections.unmodifiableMap(new TreeMap<>(state.weightSums()));

        return new AggregationResult(limited, state.duplicates(), state.perMarkContributions(), state.perMarkDecayApplied(), rawScoresCopy, weightSumsCopy, missingMarkMappings, state.weightAnomalies());
    }
}
