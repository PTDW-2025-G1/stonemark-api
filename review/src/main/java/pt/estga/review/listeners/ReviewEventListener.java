package pt.estga.review.listeners;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.events.ReviewCompletedEvent;
import pt.estga.shared.enums.ValidationState;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MeterRegistry meterRegistry;
    private final MarkOccurrenceRepository occurrenceRepository;
    private final MarkRepository markRepository;
    private final MonumentRepository monumentRepository;
    private final MarkEvidenceRepository evidenceRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReviewCompleted(ReviewCompletedEvent event) {
        Long submissionId = event.submissionId();

        submissionRepository.findById(submissionId).ifPresent(submission -> {
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
                UUID mediaFileId = submission.getOriginalMediaFileId();

                if (event.markId() != null && event.monumentId() != null) {
                    linkReviewToOccurrence(submissionId, mediaFileId, event.markId(), event.monumentId(), event.decision() == ReviewDecision.APPROVED);
                }
            } catch (Exception e) {
                // Listeners should not throw - log and continue
                log.error("Post-review failure for submission {}: {}", submissionId, e.getMessage(), e);
            }
        });
    }

    private void linkReviewToOccurrence(Long submissionId, UUID mediaFileId, Long markId, Long monumentId, boolean approved) {
        MarkOccurrence occurrence = occurrenceRepository.findByMarkIdAndMonumentId(markId, monumentId)
                .orElseGet(() -> {
                    ValidationState state = approved ? ValidationState.VERIFIED : ValidationState.PROVISIONAL;
                    Mark mark = markRepository.findById(markId).orElseThrow(() -> new IllegalArgumentException("Mark not found"));
                    Monument monument = monumentRepository.findById(monumentId).orElseThrow(() -> new IllegalArgumentException("Monument not found"));

                    return occurrenceRepository.save(MarkOccurrence.builder()
                            .mark(mark)
                            .monument(monument)
                            .validationState(state)
                            .build());
                });

        if (mediaFileId == null) return;

        try {
            evidenceRepository.findByFileId(mediaFileId).ifPresentOrElse(ev -> {
                ev.setOccurrence(occurrence);
                evidenceRepository.save(ev);
            }, () -> {
                processingRepository.findBySubmissionId(submissionId).ifPresentOrElse(proc -> {
                    float[] embedding = proc.getEmbedding();
                    MarkEvidence newEvidence = MarkEvidence.builder()
                            .fileId(mediaFileId)
                            .occurrence(occurrence)
                            .embedding(embedding)
                            .build();
                    evidenceRepository.save(newEvidence);
                    log.info("Created new MarkEvidence {} for file {} with embedding (submission {})", newEvidence.getId(), mediaFileId, submissionId);
                }, () ->
                    log.warn("Cannot create MarkEvidence for file {}: no processing record found for submission {}", mediaFileId, submissionId)
                );
            });
        } catch (Exception e) {
            log.error("Failed to link evidence for file {}: {}", mediaFileId, e.getMessage(), e);
        }
    }

    private void updateStatus(pt.estga.intake.entities.MarkEvidenceSubmission submission, SubmissionStatus target, ReviewDecision decision) {
        if (submission.getStatus() != target) {
            if (target == SubmissionStatus.PROCESSED) submission.markProcessed();
            else if (target == SubmissionStatus.REJECTED) submission.markRejected();

            submissionRepository.save(submission);
            try {
                meterRegistry.counter("review.applied", "decision", decision.name()).increment();
            } catch (Exception ex) {
                log.debug("Failed to increment review.applied metric: {}", ex.getMessage());
            }
        }
    }
}
