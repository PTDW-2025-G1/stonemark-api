package pt.estga.processing.models;

import java.util.UUID;

/**
 * Domain-shaped candidate evidence produced by the DB sanitizer.
 */
public record CandidateEvidence(UUID evidenceId, Long occurrenceId, double similarity) {
}
