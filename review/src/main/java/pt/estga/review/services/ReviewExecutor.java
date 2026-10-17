package pt.estga.review.services;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.shared.core.utils.SecurityUtils;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewExecutor {

    private final MarkEvidenceReviewRepository reviewRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public MarkEvidenceReview execute(
            Long submissionId,
            ReviewDecision decision,
            String comment,
            ResolutionResult resolution) {

        Long selectedMarkId = resolution != null && resolution.mark() != null ? resolution.mark().id() : null;

        MarkEvidenceReview review = MarkEvidenceReview.builder()
                .submissionId(submissionId)
                .selectedMarkId(selectedMarkId)
                .decision(decision)
                .reviewedAt(Instant.now())
                .comment(comment)
                .build();

        SecurityUtils.getCurrentUserId().ifPresent(review::setReviewedById);

        MarkEvidenceReview saved = reviewRepository.save(review);

        try {
            meterRegistry.counter("review.decisions.count", "decision", review.getDecision().name()).increment();
        } catch (Exception ex) {
            log.debug("Failed to record review metrics: {}", ex.getMessage());
        }

        return saved;
    }
}
