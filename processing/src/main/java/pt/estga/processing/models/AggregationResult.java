package pt.estga.processing.models;

import lombok.Builder;
import pt.estga.mark.entities.Mark;

import java.util.Map;

@Builder
public record AggregationResult(
        Map<Long, Double> scores,
        Map<Long, Double> weightSums,
        Map<Long, Mark> marksById,
        int duplicates,
        int perMarkContributions,
        int perMarkDecayApplied
) {
}