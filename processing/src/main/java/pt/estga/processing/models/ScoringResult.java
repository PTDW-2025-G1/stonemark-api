package pt.estga.processing.models;

import java.util.Map;

public record ScoringResult(
        Map<Long, Double> scores,
        Map<Long, Double> weightSums,
        int perMarkContributions,
        int perMarkDecayApplied,
        int fanOutContributionCount,
        int weightAnomalies
) {
}
