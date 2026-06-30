package pt.estga.processing.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.processing.AsyncProcessingService;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmittedListener {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final AsyncProcessingService asyncProcessingService;

    @Transactional
    @ApplicationModuleListener
    public void enrichMarkEvidence(MarkEvidenceSubmittedEvent event) {
        Long submissionId = event.submissionId();
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
}
