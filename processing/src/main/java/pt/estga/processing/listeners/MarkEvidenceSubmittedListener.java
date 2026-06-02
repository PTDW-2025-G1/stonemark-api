package pt.estga.processing.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.ProcessingOutboxRepository;
import pt.estga.processing.services.processing.AsyncProcessingService;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmittedListener {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final ProcessingOutboxRepository outboxRepository;
    private final AsyncProcessingService asyncProcessingService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enrichMarkEvidence(MarkEvidenceSubmittedEvent event) {
        Long submissionId = event.getSubmissionId();
        log.info("Submission received, ensuring queued draft for ID: {}", submissionId);

        if (processingRepository.existsBySubmissionId(submissionId)) {
            return;
        }

        MarkEvidenceProcessing placeholder = MarkEvidenceProcessing.builder()
                .submissionId(submissionId)
                .status(ProcessingStatus.PENDING)
                .updatedAt(Instant.now())
                .build();
        processingRepository.save(placeholder);
        log.info("Created placeholder processing {} for submission {}", placeholder.getId(), submissionId);

        outboxRepository.findBySubmissionId(submissionId).ifPresentOrElse(entry -> {
            entry.setStatus(ProcessingStatus.DISPATCHED);
            entry.setLastRetryAt(Instant.now());
            outboxRepository.save(entry);
            log.info("Outbox {} for submission {} marked DISPATCHED by listener", entry.getId(), submissionId);
        }, () -> log.warn("Outbox entry not found for submission {} — poller will retry", submissionId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submissionRepository.findById(submissionId).ifPresentOrElse(submission -> {
                    asyncProcessingService.processAsync(submission.getId());
                }, () -> log.warn("Submission {} not found for processing dispatch", submissionId));
            }
        });
    }
}
