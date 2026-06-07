package pt.estga.review.services;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.events.ReviewCompletedEvent;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.sharedcore.utils.SecurityUtils;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewExecutor {

    private final MarkEvidenceReviewRepository reviewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MarkSuggestionRepository suggestionRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public MarkEvidenceReview execute(
            MarkEvidenceSubmission submission,
            ReviewDecision decision,
            String comment,
            ResolutionResult resolution,
            UUID processingId) {

        Long selectedMarkId = resolution != null && resolution.mark() != null ? resolution.mark().getId() : null;

        MarkEvidenceReview review = MarkEvidenceReview.builder()
                .submissionId(submission.getId())
                .selectedMarkId(selectedMarkId)
                .decision(decision)
                .reviewedAt(Instant.now())
                .comment(comment)
                .build();

        SecurityUtils.getCurrentUserId().ifPresent(review::setReviewedById);

        MarkEvidenceReview saved = reviewRepository.save(review);

        try {
            meterRegistry.counter("review.decisions.count", "decision", review.getDecision().name()).increment();
            if (review.getDecision() == ReviewDecision.APPROVED && review.getSelectedMarkId() != null) {
                suggestionRepository.findByProcessingIdAndMarkId(processingId, review.getSelectedMarkId())
                        .ifPresent(s -> meterRegistry.summary("review.accepted.confidence").record(s.getConfidence()));
            }
        } catch (Exception ex) {
        }

        Long markId = (resolution != null && resolution.mark() != null) ? resolution.mark().getId() : null;

        SubmissionStatus targetStatus = (decision == ReviewDecision.APPROVED)
                ? SubmissionStatus.PROCESSED : SubmissionStatus.REJECTED;

        eventPublisher.publishEvent(ReviewCompletedEvent.builder()
                .submissionId(submission.getId())
                .reviewId(saved.getId())
                .decision(decision)
                .markId(markId)
                .resultingSubmissionStatus(targetStatus)
                .build());

        return saved;
    }
}
