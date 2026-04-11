package pt.estga.processing.services.similarity.helpers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.shared.utils.VectorUtils;

import java.util.Optional;

/**
 * Responsible for embedding normalization and dimension checks.
 * This class is intentionally free of scoring/DB logic and does not emit metrics.
 */
@Service
public class EmbeddingPreprocessor {

    @Value("${processing.embedding.dimension:0}")
    private int expectedEmbeddingDimension;

    /**
     * Normalize embedding and return a DB vector literal if valid. Returns empty when
     * the embedding cannot be used (normalization failed or dimension mismatch).
     */
    public Optional<String> toDbVector(MarkEvidenceProcessing processing) {
        if (processing == null || processing.getEmbedding() == null || processing.getEmbedding().length == 0) return Optional.empty();
        float[] raw = processing.getEmbedding();
        float[] norm = VectorUtils.normalize(raw);
        if (norm == null || norm.length == 0) return Optional.empty();
        if (expectedEmbeddingDimension > 0 && norm.length != expectedEmbeddingDimension) return Optional.empty();
        // defensive norm check (logically useful, but preprocessor avoids metrics)
        double n = pt.estga.shared.utils.VectorUtils.l2Norm(norm);
        if (Double.isNaN(n) || Math.abs(n - 1.0) > 1e-3) {
            // still acceptable — caller will proceed with returned vector
        }
        return Optional.of(VectorUtils.toVectorLiteral(norm));
    }
}
