package pt.estga.mark.repositories.projections;

import java.util.UUID;

/**
 * Projection for returning mark_evidence id, occurrence id and distance from a pgvector similarity query.
 */
public interface MarkEvidenceDistanceProjection {

    UUID getId();

    Long getOccurrenceId();

    /**
     * Similarity score returned by the DB (higher is better). This replaces the previous
     * 'distance' field to make the DB contract explicit: SQL should compute similarity
     * (for example 1 - cosine_distance) and return it here.
     */
    Double getSimilarity();
}
