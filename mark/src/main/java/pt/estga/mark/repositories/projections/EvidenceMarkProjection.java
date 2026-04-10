package pt.estga.mark.repositories.projections;

import pt.estga.mark.entities.Mark;

import java.util.UUID;

/**
 * Projection that returns an evidence id and its associated Mark (lightweight mapping).
 */
public interface EvidenceMarkProjection {
    UUID getId();
    Mark getMark();
}
