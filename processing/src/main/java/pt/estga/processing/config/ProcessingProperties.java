package pt.estga.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "processing")
public record ProcessingProperties(Similarity similarity, Embedding embedding) {

    public record Similarity(int maxK, double minScore) {
        public Similarity {
            if (maxK < 1) throw new IllegalArgumentException("maxK must be >= 1: " + maxK);
            if (Double.isNaN(minScore) || minScore < 0.0 || minScore > 1.0) {
                throw new IllegalArgumentException("minScore must be in [0.0,1.0]: " + minScore);
            }
        }

        public double maxDistance() {
            return Math.max(0.0, 1.0 - minScore);
        }
    }

    public record Embedding(int dimension) {}
}
