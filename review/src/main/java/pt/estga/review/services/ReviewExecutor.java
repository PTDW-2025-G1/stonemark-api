package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.services.markevidencereview.MarkEvidenceReviewCommandService;
import pt.estga.shared.utils.SecurityUtils;
import pt.estga.user.services.UserQueryService;
import org.springframework.context.ApplicationEventPublisher;
import pt.estga.review.events.ReviewCompletedEvent;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewExecutor {

    private final MarkEvidenceReviewCommandService reviewCommandService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserQueryService userQueryService;
    private final MarkSuggestionQueryService suggestionQueryService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public MarkEvidenceReview execute(
            MarkEvidenceSubmission submission,
            ReviewDecision decision,
            String comment,
            ResolutionResult resolution,
            UUID processingId) {

        MarkEvidenceReview review = MarkEvidenceReview.builder()
                .submission(submission)
                .selectedMark(resolution != null ? resolution.mark() : null)
                .decision(decision)
                .reviewedAt(Instant.now())
                .comment(comment)
                .build();

        SecurityUtils.getCurrentUserId()
                .flatMap(userQueryService::findById)
                .ifPresent(review::setReviewedBy);

        MarkEvidenceReview saved = reviewCommandService.create(review);

        // Metrics
        try {
            meterRegistry.counter("review.decisions.count", "decision", review.getDecision().name()).increment();
            if (review.getDecision() == ReviewDecision.APPROVED && review.getSelectedMark() != null) {
                suggestionQueryService.findByProcessingIdAndMarkId(processingId, review.getSelectedMark().getId())
                        .ifPresent(s -> meterRegistry.summary("review.accepted.confidence").record(s.getConfidence()));
            }
        } catch (Exception ex) {
            // metrics should not block core flow
        }

        Long markId = (resolution != null && resolution.mark() != null) ? resolution.mark().getId() : null;
        Long monumentId = (resolution != null && resolution.monument() != null) ? resolution.monument().getId() : null;

        SubmissionStatus targetStatus = (decision == ReviewDecision.APPROVED)
                ? SubmissionStatus.PROCESSED : SubmissionStatus.REJECTED;

        eventPublisher.publishEvent(ReviewCompletedEvent.builder()
                .submissionId(submission.getId())
                .reviewId(saved.getId())
                .decision(decision)
                .markId(markId)
                .monumentId(monumentId)
                .resultingSubmissionStatus(targetStatus)
                .build());

        return saved;
    }
}
