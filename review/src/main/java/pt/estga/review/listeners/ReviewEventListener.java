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
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.mark.services.occurrence.MarkOccurrenceCommandService;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.events.ReviewCompletedEvent;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final MarkEvidenceSubmissionCommandService submissionCommandService;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MeterRegistry meterRegistry;
    private final MarkOccurrenceCommandService occurrenceCommandService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReviewCompleted(ReviewCompletedEvent event) {
        Long submissionId = event.submissionId();

        submissionQueryService.findById(submissionId).ifPresent(submission -> {
            try {
                // 1) Update submission status (if needed) and record metric
                updateStatus(submission, event.resultingSubmissionStatus(), event.decision());

                // 2) Transition processing to REVIEWED if present. Use update to avoid loading the embedding vector.
                try {
                    int updated = processingRepository.updateStatusBySubmissionId(submissionId, ProcessingStatus.REVIEWED);
                    if (updated > 0) {
                        log.info("Processing marked REVIEWED for submission {} (rows updated={})", submissionId, updated);
                    }
                } catch (Exception ex) {
                    log.error("Failed to mark processing REVIEWED for submission {}: {}", submissionId, ex.getMessage(), ex);
                }

                // 3) Link review to occurrence and attach evidence (if applicable)
                UUID mediaFileId = (submission.getOriginalMediaFile() != null) ? submission.getOriginalMediaFile().getId() : null;
                boolean approved = event.decision() == ReviewDecision.APPROVED;

                if (event.markId() != null && event.monumentId() != null) {
                    occurrenceCommandService.linkReviewToOccurrence(mediaFileId, event.markId(), event.monumentId(), approved);
                }
            } catch (Exception e) {
                // Listeners should not throw - log and continue
                log.error("Post-review failure for submission {}: {}", submissionId, e.getMessage(), e);
            }
        });
    }

    private void updateStatus(pt.estga.intake.entities.MarkEvidenceSubmission submission, SubmissionStatus target, ReviewDecision decision) {
        if (submission.getStatus() != target) {
            if (target == SubmissionStatus.PROCESSED) submission.markProcessed();
            else if (target == SubmissionStatus.REJECTED) submission.markRejected();

            submissionCommandService.update(submission);
            try {
                meterRegistry.counter("review.applied", "decision", decision.name()).increment();
            } catch (Exception ex) {
                log.debug("Failed to increment review.applied metric: {}", ex.getMessage());
            }
        }
    }
}
