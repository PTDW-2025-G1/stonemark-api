package pt.estga.processing.config.policies;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SimilarityPolicy {

    private final boolean parityEnabled;
    private final int maxK;
    private final double minScore;

    public SimilarityPolicy(
            @Value("${processing.similarity.parity-check.enabled:false}") boolean parityEnabled,
            @Value("${processing.similarity.max-k:200}") int maxK,
            @Value("${processing.similarity.min-score:0.6}") double minScore
    ) {
        this.parityEnabled = parityEnabled;
        this.maxK = maxK;
        this.minScore = minScore;
    }

    public double getMaxDistance() {
        return Math.max(0.0, 1.0 - minScore);
    }
}
