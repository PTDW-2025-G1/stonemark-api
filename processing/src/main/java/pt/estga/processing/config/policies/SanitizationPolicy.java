package pt.estga.processing.config.policies;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sanitization policy for candidate evidence normalization and bounds.
 */
@Getter
@Component
public class SanitizationPolicy {

    private final double minSimilarity;
    private final double maxSimilarity;

    public SanitizationPolicy(
            @Value("${processing.similarity.min-similarity:0.0}") double minSimilarity,
            @Value("${processing.similarity.max-similarity:1.0}") double maxSimilarity
    ) {
        // Validate bounds and fail fast if configuration is inconsistent.
        if (Double.isNaN(minSimilarity) || Double.isNaN(maxSimilarity)) {
            throw new IllegalArgumentException("SanitizationPolicy: min/max similarity must be numbers");
        }
        if (minSimilarity < 0.0 || maxSimilarity > 1.0) {
            throw new IllegalArgumentException("SanitizationPolicy: similarity bounds must be within [0.0, 1.0]");
        }
        if (minSimilarity > maxSimilarity) {
            throw new IllegalArgumentException("SanitizationPolicy: minSimilarity (" + minSimilarity + ") > maxSimilarity (" + maxSimilarity + ")");
        }
        this.minSimilarity = minSimilarity;
        this.maxSimilarity = maxSimilarity;
    }
}
