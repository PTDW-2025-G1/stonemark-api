package pt.estga.intake.api;

import java.util.UUID;

import java.util.Optional;

/**
 * Public contract other modules use to read submissions and advance their status,
 * without reaching into intake's repositories or entities.
 */
public interface IntakeSubmissionApi {

    Optional<SubmissionRef> findById(UUID submissionId);

    void markProcessed(UUID submissionId);

    void markRejected(UUID submissionId);
}
