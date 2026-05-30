package pt.estga.processing.services.similarity.aggregation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.MarkScore;
import pt.estga.processing.models.AggregationResult;

import java.util.*;

@Component
@Slf4j
public class AggregationResultBuilder {

    public AggregationResult build(AggregationState state, Set<Long> validMarkIds, int k, int missingMarkMappings) {
        List<MarkScore> topScores = new ArrayList<>();
        final double MIN_WEIGHT = 1e-12;
        for (Map.Entry<Long, Double> entry : state.scores().entrySet()) {
            Long markId = entry.getKey();
            double totalScore = entry.getValue();
            Double weight = state.weightSums().get(markId);
            double conf;
            if (weight == null || weight <= MIN_WEIGHT) {
                conf = 0.0;
            } else {
                conf = totalScore / weight;
                conf = Math.max(0.0, Math.min(1.0, conf));
            }
            if (!validMarkIds.contains(markId)) continue;
            double quantized = Math.round(conf * 1_000_000d) / 1_000_000d;
            topScores.add(new MarkScore(markId, quantized));
        }

        sortDeterministically(topScores);

        List<MarkScore> limited = (k > 0 && topScores.size() > k) ? topScores.subList(0, k) : topScores;

        Map<Long, Double> rawScoresCopy = Collections.unmodifiableMap(new TreeMap<>(state.scores()));
        Map<Long, Double> weightSumsCopy = Collections.unmodifiableMap(new TreeMap<>(state.weightSums()));

        if (log.isDebugEnabled()) {
            if (state.weightAnomalies() > 0 || state.fanOutContributionCount() > 0) {
                log.debug("Aggregation diagnostics - duplicates={}, perMarkContrib={}, perMarkDecayApplied={}, fanOutContribCount={}, weightAnomalies={}",
                        state.duplicates(), state.perMarkContributions(), state.perMarkDecayApplied(), state.fanOutContributionCount(), state.weightAnomalies());
            }
        }

        final double WARN_MIN_WEIGHT = 1e-12;
        boolean anyWeight = weightSumsCopy.values().stream().anyMatch(w -> Double.isFinite(w) && w > WARN_MIN_WEIGHT);
        if (!anyWeight && !rawScoresCopy.isEmpty()) {
            log.warn("All aggregated weights are below {} ({} marks) — final confidences will be 0. This may indicate upstream filtering or numeric underflow.", WARN_MIN_WEIGHT, rawScoresCopy.size());
        }

        return new AggregationResult(limited, state.duplicates(), state.perMarkContributions(), state.perMarkDecayApplied(), state.fanOutContributionCount(), rawScoresCopy, weightSumsCopy, missingMarkMappings, state.weightAnomalies());
    }

    public static void sortDeterministically(List<MarkScore> topScores) {
        topScores.sort((a, b) -> {
            int cmp = Double.compare(b.confidence(), a.confidence());
            if (cmp != 0) return cmp;
            if (a.markId() == null && b.markId() == null) return 0;
            if (a.markId() == null) return 1;
            if (b.markId() == null) return -1;
            return a.markId().compareTo(b.markId());
        });
    }
}
