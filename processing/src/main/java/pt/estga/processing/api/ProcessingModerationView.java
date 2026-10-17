package pt.estga.processing.api;

import pt.estga.processing.enums.ProcessingStatus;

import java.util.UUID;

/**
 * Moderation view of a processing record and its best suggestion confidence.
 */
public record ProcessingModerationView(
        UUID processingId,
        UUID submissionId,
        ProcessingStatus status,
        Double maxConfidence
) {
}
