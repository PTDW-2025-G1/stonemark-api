package pt.estga.mark.repositories.projections;

import pt.estga.mark.entities.Mark;

import java.util.UUID;

/**
 * Projection exposing evidence id, embedding and associated mark.
 */
public interface EvidenceEmbeddingProjection {
    UUID getId();
    float[] getEmbedding();
    Mark getMark();
}
