package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;
import pt.estga.processing.services.enrichers.Enricher;

import java.util.List;

/**
 * Orchestrates multiple enrichment strategies for a single submission.
 * <p>
 * This service delegates to one or more {@link Enricher} implementations. Each
 * enricher runs in its own REQUIRES_NEW transaction so failures in one
 * enrichment do not affect others.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentService {

    private final List<Enricher> enrichers;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final DraftMarkEvidenceQueryService draftQueryService;
    private final MarkEvidenceSubmissionQueryService submissionQueryService;

    /**
     * Enrich the submission by delegating to configured enrichers.
     * This method is executed asynchronously so intake callers are not blocked.
     * The method runs with REQUIRES_NEW to ensure it executes after the
     * origin transaction commits. Individual enrichers also run in their own
     * REQUIRES_NEW transactions.
     *
     * @param submissionId id of the submission to enrich
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrichSubmission(Long submissionId) {
        // Ensure there is a draft linked to the submission and obtain its id.
        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            DraftMarkEvidence draft = draftQueryService.findBySubmissionId(submissionId)
                    .orElseGet(() -> draftCommandService.createIfMissingForSubmission(
                            DraftMarkEvidence.builder().submission(submission).build()
                    ));

            // Idempotency: if processing already completed or draft inactive, skip re-processing.
            if (draft.getProcessingStatus() == ProcessingStatus.COMPLETED) {
                log.info("Draft {} already COMPLETED - skipping enrichment", draft.getId());
                return;
            }

            if (Boolean.FALSE.equals(draft.getActive())) {
                log.info("Draft {} is inactive - skipping enrichment", draft.getId());
                return;
            }

            // Recovery: if a previous run left the draft IN_PROGRESS for too long, consider it FAILED and proceed.
            if (draft.getProcessingStatus() == ProcessingStatus.IN_PROGRESS) {
                // Use lastModifiedAt from AuditedEntity as heuristic when available.
                if (draft.getLastModifiedAt() != null && draft.getLastModifiedAt().isBefore(java.time.Instant.now().minus(java.time.Duration.ofMinutes(10)))) {
                    log.warn("Draft {} was IN_PROGRESS for too long - marking FAILED and attempting reprocess", draft.getId());
                    draft.setProcessingStatus(ProcessingStatus.FAILED);
                    draft.setProcessingError("Stale IN_PROGRESS state detected; marking FAILED for recovery");
                    draftCommandService.update(draft);
                } else {
                    log.info("Draft {} is already IN_PROGRESS - skipping concurrent enrichment", draft.getId());
                    return;
                }
            }

            // Mark the draft as in-progress and persist the change before running enrichers.
            draft.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
            draftCommandService.update(draft);

            Long draftId = draft.getId();

            boolean allSuccess = true;

            for (Enricher enricher : enrichers) {
                try {
                    enricher.enrich(draftId);
                } catch (Exception e) {
                    allSuccess = false;
                    log.warn("Enricher {} failed for draft {} - recording processing error and continuing", enricher.getClass().getSimpleName(), draftId, e);

                    // Append error message while preserving existing errors.
                    draftQueryService.findByIdForUpdate(draftId).ifPresent(d -> {
                        d.setProcessingStatus(ProcessingStatus.FAILED);
                        String existing = d.getProcessingError();
                        String appended = (existing == null || existing.isEmpty()) ? e.getMessage() : existing + "\n" + e.getMessage();
                        d.setProcessingError(appended);
                        draftCommandService.update(d);
                    });
                }
            }

            // If all enrichers succeeded mark as COMPLETED, otherwise mark as FAILED.
            if (allSuccess) {
                draft.setProcessingStatus(ProcessingStatus.COMPLETED);
                draft.setProcessingError(null);
            } else {
                draft.setProcessingStatus(ProcessingStatus.FAILED);
                // processingError already accumulated per-enricher
            }

            draftCommandService.update(draft);
        }, () -> log.warn("Submission with id {} not found - skipping enrichment", submissionId));
    }
}
