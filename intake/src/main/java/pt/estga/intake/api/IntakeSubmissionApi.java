package pt.estga.intake.api;

import java.util.Optional;

/**
 * Public contract other modules use to read submissions and advance their status,
 * without reaching into intake's repositories or entities.
 */
public interface IntakeSubmissionApi {

    Optional<SubmissionRef> findById(Long submissionId);

    void markProcessed(Long submissionId);

    void markRejected(Long submissionId);
}
