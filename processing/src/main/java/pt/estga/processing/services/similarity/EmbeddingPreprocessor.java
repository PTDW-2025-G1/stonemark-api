package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.estga.processing.config.policies.EmbeddingPolicy;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.shared.utils.VectorUtils;

import java.util.Optional;

/**
 * Responsible for embedding normalization and dimension checks.
 * This class is intentionally free of scoring/DB logic and does not emit metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingPreprocessor {

    private final EmbeddingPolicy embeddingPolicy;
    // Configuration values
    private int expectedEmbeddingDimensionLocal;

    @PostConstruct
    void initLocalProperties() {
        this.expectedEmbeddingDimensionLocal = embeddingPolicy.getDimension();
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
