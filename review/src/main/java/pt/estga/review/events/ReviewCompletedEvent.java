package pt.estga.review.events;

import lombok.Getter;
import pt.estga.review.enums.ReviewDecision;

import java.util.Optional;

/**
 * Event published after a review entity is successfully persisted.
 * Handlers should perform side-effects such as updating submission and processing states.
 */
public final class ReviewCompletedEvent {

    @Getter
    private final Long submissionId;
    @Getter
    private final ReviewDecision decision;
    private final Long selectedMarkId;
    private final Long reviewedById;

    public ReviewCompletedEvent(Long submissionId, ReviewDecision decision, Long selectedMarkId, Long reviewedById) {
        this.submissionId = submissionId;
        this.decision = decision;
        this.selectedMarkId = selectedMarkId;
        this.reviewedById = reviewedById;
    }

    public Optional<Long> getSelectedMarkId() {
        return Optional.ofNullable(selectedMarkId);
    }

    public Optional<Long> getReviewedById() {
        return Optional.ofNullable(reviewedById);
    }
}

