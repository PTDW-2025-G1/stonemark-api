package pt.estga.processing.config.policies;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import java.util.Locale;
import org.springframework.stereotype.Component;
import pt.estga.processing.enums.FanOutStrategy;

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
    private final FanOutStrategy fanOutStrategy;

    public ScoringPolicy(
            @Value("${processing.similarity.use-rank-weighting:true}") boolean useRankWeighting,
            @Value("${processing.similarity.per-mark-decay:0.5}") double perMarkDecay,
            @Value("${processing.similarity.fanout-strategy:SPLIT}") String fanOutStrategy
    ) {
        // Validate perMarkDecay: must be within [0.0, 1.0]
        if (Double.isNaN(perMarkDecay) || perMarkDecay < 0.0 || perMarkDecay > 1.0) {
            throw new IllegalArgumentException("Invalid perMarkDecay: " + perMarkDecay + " — expected value in [0.0, 1.0]");
        }
        this.useRankWeighting = useRankWeighting;
        this.perMarkDecay = perMarkDecay;
        try {
            this.fanOutStrategy = FanOutStrategy.valueOf(fanOutStrategy.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid fanout strategy: " + fanOutStrategy + " — expected SPLIT or FULL");
        }
    }
}
