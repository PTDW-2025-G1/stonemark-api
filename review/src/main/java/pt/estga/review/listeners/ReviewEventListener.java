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
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.events.ReviewCompletedEvent;
import pt.estga.commoncore.enums.ValidationState;

import java.util.Optional;
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
    private final MarkEvidenceRepository evidenceRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReviewCompleted(ReviewCompletedEvent event) {
        Long submissionId = event.submissionId();

        submissionRepository.findById(submissionId).ifPresent(submission -> {
            try {
                updateStatus(submission, event.resultingSubmissionStatus(), event.decision());

                try {
                    int updated = processingRepository.updateStatusBySubmissionId(submissionId, ProcessingStatus.REVIEWED);
                    if (updated > 0) {
                        log.info("Processing marked REVIEWED for submission {} (rows updated={})", submissionId, updated);
                    }
                } catch (Exception ex) {
                    log.error("Failed to mark processing REVIEWED for submission {}: {}", submissionId, ex.getMessage(), ex);
                }

                if (event.markId() != null) {
                    UUID mediaFileId = submission.getOriginalMediaFileId();
                    linkEvidenceToMark(submissionId, mediaFileId, event.markId(), event.decision() == ReviewDecision.APPROVED);
                }
            } catch (Exception e) {
                log.error("Post-review failure for submission {}: {}", submissionId, e.getMessage(), e);
            }
        });
    }

    private void linkEvidenceToMark(Long submissionId, UUID mediaFileId, Long markId, boolean approved) {
        if (mediaFileId == null) return;

        MarkOccurrence occurrence = occurrenceRepository.findByMarkIdAndMonumentIdIsNull(markId)
                .orElseGet(() -> {
                    ValidationState state = approved ? ValidationState.VERIFIED : ValidationState.PROVISIONAL;
                    Mark mark = markRepository.findById(markId).orElseThrow(() -> new IllegalArgumentException("Mark not found"));

                    return occurrenceRepository.save(MarkOccurrence.builder()
                            .mark(mark)
                            .monument(null)
                            .validationState(state)
                            .build());
                });

        try {
            evidenceRepository.findByFileId(mediaFileId).ifPresentOrElse(ev -> {
                ev.setOccurrence(occurrence);
                evidenceRepository.save(ev);
            }, () -> {
                float[] embedding = parseEmbedding(processingRepository.findEmbeddingTextBySubmissionId(submissionId));
                if (embedding == null) {
                    log.warn("Cannot create MarkEvidence for file {}: no embedding for submission {}", mediaFileId, submissionId);
                    return;
                }
                MarkEvidence newEvidence = MarkEvidence.builder()
                        .fileId(mediaFileId)
                        .occurrence(occurrence)
                        .embedding(embedding)
                        .build();
                evidenceRepository.save(newEvidence);
                log.info("Created new MarkEvidence {} for file {} with embedding (submission {})", newEvidence.getId(), mediaFileId, submissionId);
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

    private static float[] parseEmbedding(Optional<String> text) {
        return text.map(t -> {
            String trimmed = t.trim();
            if (trimmed.isEmpty() || "[]".equals(trimmed)) return null;
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            String[] parts = trimmed.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        }).orElse(null);
    }
}
