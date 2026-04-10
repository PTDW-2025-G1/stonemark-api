package pt.estga.processing.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.ProcessingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmittedListener {

    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final ProcessingService processingService;

    /**
     * After a submission is committed, ensure a processing draft exists and is queued.
     * Actual enrichment is performed by the scheduled orchestrator.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enrichMarkEvidence(MarkEvidenceSubmittedEvent event) {
        Long submissionId = event.getSubmissionId();
        log.info("Submission received, ensuring queued draft for ID: {}", submissionId);

        if (processingRepository.existsBySubmissionId(submissionId)) {
            return; // already processed or in progress
        }

        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            // Kick off processing (idempotent). ProcessingService will handle retries and failures.
            processingService.processSubmission(submissionId);
        }, () -> log.warn("Submission with id {} not found while enqueuing draft", submissionId));
    }
}
