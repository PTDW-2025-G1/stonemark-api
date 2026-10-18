package pt.estga.processing.vision;

/**
 * Outcome of a Vision analysis: whether a mark was detected and the feature embedding
 * with the provenance needed to interpret it.
 *
 * <p>The embedding model, version and dimension are optional so the API tolerates a
 * reduced or older Vision response; when they are absent the embedding is treated as
 * unversioned.
 */
public record DetectionResult(
        boolean isMasonMark,
        float[] embedding,
        String embeddingModel,
        String embeddingVersion,
        Integer dim
) {
}
