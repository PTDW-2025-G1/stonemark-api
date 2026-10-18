package pt.estga.shared.core.models;

/**
 * A feature embedding together with the provenance required to interpret it: the
 * model that produced it and that model's version. Persisted beside every vector so a
 * model swap can be detected and the vector recomputed instead of silently compared
 * against vectors from a different model.
 *
 * @param values  the embedding components
 * @param model   identifier of the embedding model (for example {@code dinov2-vits14})
 * @param version version of the model/pipeline, bumped on any change that alters vectors
 * @param dim     dimensionality, normally {@code values.length}
 */
public record EmbeddingVector(
        float[] values,
        String model,
        String version,
        Integer dim
) {

    /** An embedding with no recorded provenance; dimension is taken from the array. */
    public static EmbeddingVector of(float[] values) {
        return new EmbeddingVector(values, null, null, values == null ? null : values.length);
    }

    public boolean isEmpty() {
        return values == null || values.length == 0;
    }
}
