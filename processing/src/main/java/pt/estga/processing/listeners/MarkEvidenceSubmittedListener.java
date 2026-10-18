package pt.estga.processing.listeners;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.processing.AsyncProcessingService;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "processing.enabled", havingValue = "true")
public class MarkEvidenceSubmittedListener {

    private final IntakeSubmissionApi intakeApi;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final AsyncProcessingService asyncProcessingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enrichMarkEvidence(MarkEvidenceSubmittedEvent event) {
        UUID submissionId = event.submissionId();
        log.info("Submission received, ensuring queued draft for ID: {}", submissionId);

        boolean needsDispatch = false;

        var existing = processingRepository.findBySubmissionId(submissionId);
        if (existing.isPresent()) {
            var p = existing.get();
            if (p.getStatus() == ProcessingStatus.COMPLETED || p.getStatus() == ProcessingStatus.PROCESSING) {
                log.debug("Submission {} already {} — skipping", submissionId, p.getStatus());
                return;
            }
            log.info("Submission {} exists as {} — re-dispatching", submissionId, p.getStatus());
            needsDispatch = true;
        } else {
            MarkEvidenceProcessing placeholder = MarkEvidenceProcessing.builder()
                    .submissionId(submissionId)
                    .status(ProcessingStatus.PENDING)
                    .updatedAt(Instant.now())
                    .build();
            processingRepository.save(placeholder);
            log.info("Created placeholder processing {} for submission {}", placeholder.getId(), submissionId);
            needsDispatch = true;
        }

        if (needsDispatch) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dispatch(submissionId);
                    }
                });
            } else {
                dispatch(submissionId);
            }
        }
    }

    private void dispatch(UUID submissionId) {
        intakeApi.findById(submissionId).ifPresentOrElse(
                submission -> asyncProcessingService.processAsync(submission.id()),
                () -> log.warn("Submission {} not found for processing dispatch", submissionId));
    }
}
