package pt.estga.processing.models;

import java.util.Map;

public record AggregationState(
        Map<Long, Double> scores,
        Map<Long, Double> weightSums,
        Map<Long, Double> confidences,
        int duplicates,
        int perMarkContributions,
        int perMarkDecayApplied,
        int weightAnomalies
) {
}