package pt.estga.processing.config;

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

    public SanitizationPolicy(@Value("${processing.similarity.min-similarity:0.0}") double minSimilarity,
                              @Value("${processing.similarity.max-similarity:1.0}") double maxSimilarity) {
        this.minSimilarity = minSimilarity;
        this.maxSimilarity = maxSimilarity;
    }
}
