package pt.estga.processing.repositories.projections;

import pt.estga.processing.enums.ProcessingStatus;

import java.util.UUID;

/**
 * Projection used by moderation queries to return processing, submission and confidence info.
 */
public interface ProcessingModerationProjection {
    UUID getProcessingId();
    Long getSubmissionId();
    ProcessingStatus getStatus();
    Double getMaxConfidence();
}
