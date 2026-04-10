package pt.estga.mark.repositories.projections;

import java.util.UUID;

/**
 * Projection for returning mark_evidence id, occurrence id and distance from a pgvector similarity query.
 */
public interface MarkEvidenceDistanceProjection {

    UUID getId();

    Long getOccurrenceId();

    Double getDistance();
}
