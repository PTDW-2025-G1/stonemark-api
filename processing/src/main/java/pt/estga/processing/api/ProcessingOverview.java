package pt.estga.processing.api;

import pt.estga.processing.enums.ProcessingStatus;

import java.util.UUID;

/**
 * Read-only view of a processing record's identity and status.
 */
public record ProcessingOverview(
        UUID processingId,
        ProcessingStatus status
) {
}
