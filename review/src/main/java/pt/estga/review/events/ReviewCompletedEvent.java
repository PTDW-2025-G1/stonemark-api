package pt.estga.review.events;

import lombok.Builder;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.review.enums.ReviewDecision;

/**
 * Event published after a review entity is successfully persisted.
 * The event is the single source of truth for post-review transitions: it carries
 * the submission id, review id, the decision and the explicit resulting submission status.
 */
@Builder
public record ReviewCompletedEvent(
        Long submissionId,
        Long reviewId,
        ReviewDecision decision,
        Long markId,
        Long monumentId,
        SubmissionStatus resultingSubmissionStatus
) {
}
