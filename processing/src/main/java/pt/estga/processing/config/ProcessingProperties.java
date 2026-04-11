package pt.estga.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Application configuration holder for processing-related properties.
 * Maps to properties under the "processing" prefix.
 */
@Getter
@Component
@ConfigurationProperties(prefix = "processing")
public class ProcessingProperties {

    private Similarity similarity = new Similarity();
    private Embedding embedding = new Embedding();

    public void setSimilarity(Similarity similarity) {
        this.similarity = Objects.requireNonNullElseGet(similarity, Similarity::new);
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = Objects.requireNonNullElseGet(embedding, Embedding::new);
    }

    @Getter
    public static class Similarity {
        @Setter
        private double minScore = 0.6;
        @Setter
        private boolean useRankWeighting = true;
        private ParityCheck parityCheck = new ParityCheck();
        @Setter
        private int maxK = 200;
        @Setter
        private double perMarkDecay = 0.5;

        public void setParityCheck(ParityCheck parityCheck) {
            this.parityCheck = Objects.requireNonNullElseGet(parityCheck, ParityCheck::new);
        }

        @Setter
        @Getter
        public static class ParityCheck {
            private boolean enabled = false;
            private boolean async = true;
            private double tolerance = 0.001;
            private int sampleSize = 3;
        }
    }

    @Setter
    @Getter
    public static class Embedding {
        private int dimension = 0;

    }
}
