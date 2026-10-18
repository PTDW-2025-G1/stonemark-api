package pt.estga.processing.vision;

/**
 * Outcome of a Vision analysis: the feature embedding for a submission image and the
 * provenance needed to interpret it.
 *
 * <p>The embedding model, version and dimension are optional so the API tolerates a
 * reduced or older Vision response; when they are absent the embedding is treated as
 * unversioned.
 */
public record DetectionResult(
        float[] embedding,
        String embeddingModel,
        String embeddingVersion,
        Integer dim
) {
}
