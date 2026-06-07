package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.commoncore.utils.VectorUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingPreprocessor {

    private final ProcessingProperties properties;
    private int expectedEmbeddingDimensionLocal;

    @PostConstruct
    void initLocalProperties() {
        this.expectedEmbeddingDimensionLocal = properties.embedding().dimension();
    }

    /**
     * Return a DB vector literal from a processing embedding, verifying it is already
     * normalized (enforced at storage time in ProcessingServiceImpl). Returns empty
     * when the embedding cannot be used (dimension mismatch).
     */
    public Optional<String> toDbVector(MarkEvidenceProcessing processing) {
        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) return Optional.empty();
        float[] embedding = processing.getEmbedding();
        int expectedEmbeddingDimension = expectedEmbeddingDimensionLocal;
        if (expectedEmbeddingDimension > 0 && embedding.length != expectedEmbeddingDimension) return Optional.empty();
        double n = VectorUtils.l2Norm(embedding);
        if (Double.isNaN(n) || Math.abs(n - 1.0) > 1e-3) {
            log.warn("Processing embedding has unexpected norm {} (expected ~1.0) — continuing", String.format("%.4f", n));
        }
        return Optional.of(VectorUtils.toVectorLiteral(embedding));
    }
}
