package pt.estga.processing.config.policies;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Parity check policy populated from application configuration. Keeps parity
 * related behavior in a single immutable object for easier testing and
 * separation from runtime logic.
 */
@Getter
@Component
public class ParityPolicy {
    private final boolean async;
    private final double tolerance;
    private final int sampleSize;

    public ParityPolicy(
            @Value("${processing.similarity.parity-check.async:true}") boolean async,
            @Value("${processing.similarity.parity-check.tolerance:0.001}") double tolerance,
            @Value("${processing.similarity.parity-check.sample-size:3}") int sampleSize
    ) {
        // Validate parity policy values. sampleSize must be >= 1 and tolerance >= 0.
        if (sampleSize < 1) throw new IllegalArgumentException("ParityPolicy.sampleSize must be >= 1: " + sampleSize);
        if (Double.isNaN(tolerance) || tolerance < 0.0) throw new IllegalArgumentException("ParityPolicy.tolerance must be >= 0: " + tolerance);

        this.async = async;
        this.tolerance = tolerance;
        this.sampleSize = sampleSize;
    }
}
