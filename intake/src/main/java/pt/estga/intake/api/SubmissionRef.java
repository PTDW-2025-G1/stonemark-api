package pt.estga.intake.api;

import java.util.UUID;

/**
 * Minimal, read-only view of a submission for other modules.
 */
public record SubmissionRef(
        UUID id,
        UUID originalMediaFileId,
        Double latitude,
        Double longitude
) {
}
