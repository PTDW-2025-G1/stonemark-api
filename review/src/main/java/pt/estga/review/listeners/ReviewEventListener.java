package pt.estga.review.listeners;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.services.MarkEvidenceSubmissionCommandService;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.events.ReviewCompletedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final MarkEvidenceSubmissionCommandService submissionCommandService;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MeterRegistry meterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCompleted(ReviewCompletedEvent event) {
        Long submissionId = event.getSubmissionId();
        try {
            // Set submission status according to review decision. Use explicit domain statuses
            // and make the operation idempotent: only change when a transition is necessary.
            submissionQueryService.findById(submissionId).ifPresent(s -> {
                SubmissionStatus current = s.getStatus();
                boolean updated = false;
                if (event.getDecision() == ReviewDecision.APPROVED) {
                    if (current != SubmissionStatus.PROCESSED) {
                        s.markProcessed();
                        updated = true;
                    }
                } else if (event.getDecision() == ReviewDecision.REJECTED) {
                    if (current != SubmissionStatus.REJECTED) {
                        s.markRejected();
                        updated = true;
                    }
                } else {
                    // For NO_MATCH / FLAGGED treat as processed (manual review completed)
                    if (current != SubmissionStatus.PROCESSED && current != SubmissionStatus.REJECTED) {
                        s.markProcessed();
                        updated = true;
                    }
                }
                if (updated) {
                    submissionCommandService.update(s);
                }
                // Metrics: count processed/rejected reviews (record only when applied)
                try {
                    if (updated) {
                        try {
                            meterRegistry.counter("review.event.applied.count", "decision", event.getDecision().name()).increment();
                        } catch (Exception ex) {
                            log.debug("Failed to increment review event metric for submission {}: {}", submissionId, ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Failed to increment review event metric for submission {}: {}", submissionId, ex.getMessage());
                }
            });

            // Transition processing to REVIEWED if present
            processingRepository.findBySubmissionId(submissionId).ifPresent(p -> {
                p.markReviewed();
                processingRepository.save(p);
                log.info("Processing {} marked REVIEWED for submission {}", p.getId(), submissionId);
            });
        } catch (Exception e) {
            // Listeners should not throw - log and continue
            log.error("Failed to apply post-review state transitions for submission {}: {}", submissionId, e.getMessage(), e);
        }
    }
}
