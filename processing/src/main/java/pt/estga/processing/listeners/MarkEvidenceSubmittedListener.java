package pt.estga.processing.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.entities.DraftMarkEvidence;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmittedListener {

    private final DraftMarkEvidenceCommandService draftCommandService;
    private final MarkEvidenceSubmissionQueryService submissionQueryService;

    /**
     * After a submission is committed, ensure a processing draft exists and is queued.
     * Actual enrichment is performed by the scheduled orchestrator.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enrichMarkEvidence(MarkEvidenceSubmittedEvent event) {
        Long submissionId = event.getSubmissionId();
        log.info("Submission received, ensuring queued draft for ID: {}", submissionId);

        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            DraftMarkEvidence draft = DraftMarkEvidence.builder().submission(submission).build();
            draftCommandService.createIfMissingForSubmission(draft);
        }, () -> log.warn("Submission with id {} not found while enqueuing draft", submissionId));
    }
}
