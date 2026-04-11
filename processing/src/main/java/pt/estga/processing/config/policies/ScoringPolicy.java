package pt.estga.processing.config.policies;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Immutable scoring policy derived from configuration. This separates
 * scoring decisions from aggregation logic so the aggregator becomes
 * easier to test and has no direct dependency on configuration.
 */
@Getter
@Component
public class ScoringPolicy {

    private final boolean useRankWeighting;
    private final double perMarkDecay;

    public ScoringPolicy(
            @Value("${processing.similarity.use-rank-weighting:true}") boolean useRankWeighting,
            @Value("${processing.similarity.per-mark-decay:0.5}") double perMarkDecay
    ) {
        this.useRankWeighting = useRankWeighting;
        this.perMarkDecay = perMarkDecay;
    }
}
