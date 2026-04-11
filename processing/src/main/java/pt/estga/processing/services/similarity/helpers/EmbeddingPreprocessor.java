package pt.estga.processing.services.similarity.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.estga.processing.config.EmbeddingPolicy;
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
     * Normalize embedding and return a DB vector literal if valid. Returns empty when
     * the embedding cannot be used (normalization failed or dimension mismatch).
     */
    public Optional<String> toDbVector(MarkEvidenceProcessing processing) {
        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) return Optional.empty();
        float[] raw = processing.getEmbedding();
        float[] norm = VectorUtils.normalize(raw);
        if (norm == null || norm.length == 0) return Optional.empty();
        int expectedEmbeddingDimension = expectedEmbeddingDimensionLocal;
        if (expectedEmbeddingDimension > 0 && norm.length != expectedEmbeddingDimension) return Optional.empty();
        // defensive norm check: log a warning when normalization is imperfect
        double n = pt.estga.shared.utils.VectorUtils.l2Norm(norm);
        if (Double.isNaN(n) || Math.abs(n - 1.0) > 1e-3) {
            log.warn("Processing embedding not normalized after normalization attempt (norm={}) — continuing with normalized vector", n);
        }
        return Optional.of(VectorUtils.toVectorLiteral(norm));
    }
}
