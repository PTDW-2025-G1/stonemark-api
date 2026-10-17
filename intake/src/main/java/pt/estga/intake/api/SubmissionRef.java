package pt.estga.intake.api;

import java.util.UUID;

/**
 * Minimal, read-only view of a submission for other modules.
 */
public record SubmissionRef(
        Long id,
        UUID originalMediaFileId
) {
}
