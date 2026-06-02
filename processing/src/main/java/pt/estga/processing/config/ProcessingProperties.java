package pt.estga.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pt.estga.processing.enums.FanOutStrategy;

import java.util.Locale;

@ConfigurationProperties(prefix = "processing")
public record ProcessingProperties(Similarity similarity, Embedding embedding) {

    public record Similarity(
            boolean useRankWeighting,
            double perMarkDecay,
            String fanoutStrategy,
            boolean parityEnabled,
            int maxK,
            double minScore,
            double minSimilarity,
            double maxSimilarity,
            ParityCheck parityCheck
    ) {
        public Similarity {
            if (Double.isNaN(perMarkDecay) || perMarkDecay < 0.0 || perMarkDecay > 1.0) {
                throw new IllegalArgumentException("perMarkDecay must be in [0.0,1.0]: " + perMarkDecay);
            }
            if (Double.isNaN(minScore) || minScore < 0.0 || minScore > 1.0) {
                throw new IllegalArgumentException("minScore must be in [0.0,1.0]: " + minScore);
            }
            if (maxK < 1) throw new IllegalArgumentException("maxK must be >= 1: " + maxK);
            if (Double.isNaN(minSimilarity) || Double.isNaN(maxSimilarity)
                    || minSimilarity < 0.0 || maxSimilarity > 1.0 || minSimilarity > maxSimilarity) {
                throw new IllegalArgumentException("invalid similarity bounds: [" + minSimilarity + "," + maxSimilarity + "]");
            }
        }

        public FanOutStrategy fanOutStrategy() {
            return FanOutStrategy.valueOf(fanoutStrategy.trim().toUpperCase(Locale.ROOT));
        }

        public double maxDistance() {
            return Math.max(0.0, 1.0 - minScore);
        }

        public record ParityCheck(boolean async, double tolerance, int sampleSize) {
            public ParityCheck {
                if (sampleSize < 1) throw new IllegalArgumentException("sampleSize must be >= 1: " + sampleSize);
                if (Double.isNaN(tolerance) || tolerance < 0.0) {
                    throw new IllegalArgumentException("tolerance must be >= 0: " + tolerance);
                }
            }
        }
    }

    public record Embedding(int dimension) {}
}
