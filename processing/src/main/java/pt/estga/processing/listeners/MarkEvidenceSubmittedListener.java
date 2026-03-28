package pt.estga.processing.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.processing.services.EnrichmentService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmittedListener {

    private final EnrichmentService enrichmentService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enrichMarkEvidence(MarkEvidenceSubmittedEvent event) {
        Long submissionId = event.getSubmissionId();
        log.info("Async detection processing for submission ID: {}", submissionId);

        enrichmentService.enrichSubmission(submissionId);
    }
}
