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

import java.time.Instant;
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

            Long draftId = draft.getId();

            // PRE-CHECK (no lock) - quick exits before acquiring the pessimistic lock.
            // These checks use the read model and avoid taking a lock when unnecessary.
            if (draft.getProcessingStatus() == ProcessingStatus.COMPLETED) {
                log.info("Draft {} already COMPLETED - skipping enrichment", draftId);
                return;
            }

            if (Boolean.FALSE.equals(draft.getActive())) {
                log.info("Draft {} is inactive - skipping enrichment", draftId);
                return;
            }

            if (draft.getProcessingStatus() == ProcessingStatus.IN_PROGRESS) {
                Instant last = draft.getLastModifiedAt();
                if (last != null && last.isAfter(Instant.now().minus(java.time.Duration.ofMinutes(10)))) {
                    log.info("Draft {} already IN_PROGRESS - skipping concurrent enrichment", draftId);
                    return;
                }
                // stale IN_PROGRESS will be handled after acquiring a lock
            }

            // Acquire pessimistic lock to make a safe decision. All subsequent modifications
            // will reload the entity under lock before applying changes.
            DraftMarkEvidence locked = draftQueryService.findByIdForUpdate(draftId)
                    .orElseThrow(() -> new IllegalStateException("Draft with id " + draftId + " not found"));

            // Re-check under lock for IN_PROGRESS state to handle races between the pre-check
            // and acquiring the lock. If still IN_PROGRESS and not stale, skip. If stale, record
            // a recovery note and proceed.
            if (locked.getProcessingStatus() == ProcessingStatus.IN_PROGRESS) {
                Instant last = locked.getLastModifiedAt();
                if (last != null && last.isAfter(Instant.now().minus(java.time.Duration.ofMinutes(10)))) {
                    log.info("Draft {} is already IN_PROGRESS and not stale - skipping concurrent enrichment", draftId);
                    return;
                }

                log.warn("Draft {} is IN_PROGRESS but stale (under lock) - recording recovery note and proceeding", draftId);
                appendError(draftId, "Stale IN_PROGRESS state detected; attempting recovery");
            }

            // Now mark as IN_PROGRESS (fresh load + update under lock)
            markInProgress(draftId);

            // Run all enrichers (each runs in its own REQUIRES_NEW transaction). Collect and persist errors per-enricher.
            for (Enricher enricher : enrichers) {
                try {
                    enricher.enrich(draftId);
                } catch (Exception e) {
                    log.warn("Enricher {} failed for draft {} - persisting error and continuing", enricher.getClass().getSimpleName(), draftId, e);
                    appendError(draftId, e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }

            // Finalize: reload locked draft and decide final state based only on embedding presence.
            DraftMarkEvidence finalLocked = draftQueryService.findByIdForUpdate(draftId)
                    .orElseThrow(() -> new IllegalStateException("Draft with id " + draftId + " not found while finalizing enrichment"));

            if (finalLocked.getEmbedding() == null) {
                // Ensure missing-embedding error is persisted and mark as FAILED.
                String existing = finalLocked.getProcessingError();
                String appended = (existing == null || existing.isEmpty()) ? "Missing embedding after enrichment"
                        : existing + "\n" + "Missing embedding after enrichment";
                markFailed(draftId, appended);
            } else {
                markCompleted(draftId);
            }
        }, () -> log.warn("Submission with id {} not found - skipping enrichment", submissionId));
    }

    // --- Command-style helpers that always reload under pessimistic lock and persist changes ---

    private void markInProgress(Long draftId) {
        draftQueryService.findByIdForUpdate(draftId).ifPresent(d -> {
            d.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
            // Persist the change via command service; update should merge the state.
            draftCommandService.update(d);
        });
    }

    private void appendError(Long draftId, String error) {
        if (error == null || error.isEmpty()) return;
        draftQueryService.findByIdForUpdate(draftId).ifPresent(d -> {
            String existing = d.getProcessingError();
            String appended = (existing == null || existing.isEmpty()) ? error : existing + "\n" + error;
            d.setProcessingError(appended);
            draftCommandService.update(d);
        });
    }

    private void markFailed(Long draftId, String error) {
        draftQueryService.findByIdForUpdate(draftId).ifPresent(d -> {
            d.setProcessingStatus(ProcessingStatus.FAILED);
            d.setProcessingError(error);
            draftCommandService.update(d);
        });
    }

    private void markCompleted(Long draftId) {
        draftQueryService.findByIdForUpdate(draftId).ifPresent(d -> {
            d.setProcessingStatus(ProcessingStatus.COMPLETED);
            d.setProcessingError(null);
            draftCommandService.update(d);
        });
    }
}
