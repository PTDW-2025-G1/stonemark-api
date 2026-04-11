package pt.estga.processing.models;

import java.util.List;
import java.util.Map;

/**
 * AggregationResult now contains a final ranked list of MarkScore and counters
 * describing aggregation diagnostics.
 */
public record AggregationResult(
        List<MarkScore> topScores,
        int duplicates,
        int perMarkContributions,
        int perMarkDecayApplied,
        Map<Long, Double> rawScores,
        Map<Long, Double> weightSums,
        int missingMarkMappings
) {
}