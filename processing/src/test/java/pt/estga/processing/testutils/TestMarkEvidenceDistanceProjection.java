package pt.estga.processing.testutils;

import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;

import java.util.UUID;

/**
 * Simple test implementation of the DB projection used by the sanitizer.
 */
public class TestMarkEvidenceDistanceProjection implements MarkEvidenceDistanceProjection {

    private final UUID id;
    private final Long occurrenceId;
    private final Double similarity;

    public TestMarkEvidenceDistanceProjection(UUID id, Long occurrenceId, Double similarity) {
        this.id = id;
        this.occurrenceId = occurrenceId;
        this.similarity = similarity;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Long getOccurrenceId() {
        return occurrenceId;
    }

    @Override
    public Double getSimilarity() {
        return similarity;
    }
}
