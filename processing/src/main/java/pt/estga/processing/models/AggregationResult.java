package pt.estga.processing.models;

import java.util.List;

/**
 * AggregationResult now contains a final ranked list of MarkScore and counters
 * describing aggregation diagnostics.
 */
public record AggregationResult(
        List<MarkScore> topScores,
        int duplicates,
        int perMarkContributions,
        int perMarkDecayApplied,
        java.util.Map<Long, Double> rawScores,
        java.util.Map<Long, Double> weightSums
) {
}