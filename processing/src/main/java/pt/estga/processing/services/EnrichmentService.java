package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;
import pt.estga.processing.services.enrichers.Enricher;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentService {

    private final List<Enricher> enrichers;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final DraftMarkEvidenceQueryService draftQueryService;
    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final PlatformTransactionManager txManager;

    @Async
    public void enrichSubmission(Long submissionId) {
        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            DraftMarkEvidence draft = draftQueryService.findBySubmissionId(submissionId)
                    .orElseGet(() -> draftCommandService.createIfMissingForSubmission(
                            DraftMarkEvidence.builder().submission(submission).build()
                    ));

            Long draftId = draft.getId();

            // --- PRE-CHECK (no lock) ---
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
                log.warn("Draft {} IN_PROGRESS is stale - will attempt recovery", draftId);
            }

            // --- MARK IN_PROGRESS (REQUIRES_NEW) ---
            markInProgressTx(draftId);

            // --- RUN ENRICHERS ---
            List<String> errors = new java.util.ArrayList<>();
            for (Enricher enricher : enrichers) {
                try {
                    enricher.enrich(draftId);
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    log.warn("Enricher {} failed for draft {} - collecting error", enricher.getClass().getSimpleName(), draftId, e);
                    errors.add(msg);
                }
            }

            // --- FINALIZE DRAFT (REQUIRES_NEW) ---
            finalizeDraftTx(draftId, errors);

        }, () -> log.warn("Submission with id {} not found - skipping enrichment", submissionId));
    }

    public void markInProgressTx(Long draftId) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(_status -> {
            DraftMarkEvidence draft = draftQueryService.findByIdForUpdate(draftId)
                    .orElseThrow(() -> new IllegalStateException("Draft with id " + draftId + " not found"));

            if (draft.getProcessingStatus() == ProcessingStatus.IN_PROGRESS) {
                Instant last = draft.getLastModifiedAt();
                if (last == null || last.isBefore(Instant.now().minus(java.time.Duration.ofMinutes(10)))) {
                    appendError(draft, "Stale IN_PROGRESS detected - attempting recovery");
                } else {
                    // Already in progress and not stale
                    return;
                }
            }

            draft.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
            draftCommandService.update(draft);
        });
    }

    private void appendError(DraftMarkEvidence draft, String error) {
        String existing = draft.getProcessingError();
        String merged = (existing == null || existing.isEmpty()) ? error : existing + "\n" + error;
        draft.setProcessingError(merged);
        draftCommandService.update(draft);
    }

    public void finalizeDraftTx(Long draftId, List<String> collectedErrors) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(_status -> {
            DraftMarkEvidence draft = draftQueryService.findByIdForUpdate(draftId)
                    .orElseThrow(() -> new IllegalStateException("Draft with id " + draftId + " not found during finalization"));

            // Merge errors
            String merged = draft.getProcessingError();
            if (collectedErrors != null && !collectedErrors.isEmpty()) {
                String joined = String.join("\n", collectedErrors);
                merged = (merged == null || merged.isEmpty()) ? joined : merged + "\n" + joined;
            }

            if (draft.getEmbedding() == null) {
                if (merged == null || merged.isEmpty()) merged = "Missing embedding after enrichment";
                draft.setProcessingStatus(ProcessingStatus.FAILED);
                draft.setProcessingError(merged);
            } else {
                draft.setProcessingStatus(ProcessingStatus.COMPLETED);
                draft.setProcessingError(null);
            }

            draftCommandService.update(draft);
        });
    }
}
