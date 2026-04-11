package pt.estga.processing.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Flattened processing configuration.
 *
 * Values are injected via {@code @Value} from properties on the classpath.
 * Nested keys used previously (e.g. processing.similarity.min-score) are
 * supported via nested placeholders so both kebab-case and camelCase keys
 * resolve correctly. Defaults mirror the previous hard-coded values.
 */
@Getter
@Component
public class ProcessingProperties {

    @Value("${processing.similarity.min-score:0.6}")
    private double minScore;

    @Value("${processing.similarity.use-rank-weighting:true}")
    private boolean useRankWeighting;

    @Value("${processing.similarity.max-k:200}")
    private int maxK;

    @Value("${processing.similarity.per-mark-decay:0.5}")
    private double perMarkDecay;

    @Value("${processing.similarity.parity-check.enabled:false}")
    private boolean parityEnabled;

    @Value("${processing.similarity.parity-check.async:true}")
    private boolean parityAsync;

    @Value("${processing.similarity.parity-check.tolerance:0.001}")
    private double parityTolerance;

    @Value("${processing.similarity.parity-check.sample-size:3}")
    private int paritySampleSize;

    @Value("${processing.embedding.dimension:0}")
    private int embeddingDimension;

    /**
     * Return the maximum DB distance corresponding to the configured minScore.
     */
    public double getMaxDistance() {
        return Math.max(0.0, 1.0 - minScore);
    }
}
