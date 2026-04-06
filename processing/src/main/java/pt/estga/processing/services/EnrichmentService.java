package pt.estga.processing.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;
import pt.estga.processing.services.enrichers.Enricher;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.function.Function;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class EnrichmentService {

    private final List<Enricher> enrichers;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final DraftMarkEvidenceQueryService draftQueryService;
    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final PlatformTransactionManager txManager;
    @Value("${processing.enrichment.stale-timeout-minutes:10}")
    private long staleTimeoutMinutes;
    private final Clock clock;

    /**
     * Explicit constructor to allow injection of required collaborators. The stale timeout is injected
     * via field-level configuration property `processing.enrichment.stale-timeout-minutes` (defaults to 10).
     */
    @Autowired
    public EnrichmentService(List<Enricher> enrichers,
                             DraftMarkEvidenceCommandService draftCommandService,
                             DraftMarkEvidenceQueryService draftQueryService,
                             MarkEvidenceSubmissionQueryService submissionQueryService,
                             PlatformTransactionManager txManager,
                             @Value("#{T(java.time.Clock).systemUTC()}") Clock clock) {
        this.enrichers = enrichers;
        this.draftCommandService = draftCommandService;
        this.draftQueryService = draftQueryService;
        this.submissionQueryService = submissionQueryService;
        this.txManager = txManager;
        this.clock = clock;
    }

    /**
     * Convenience constructor for tests to allow passing a custom stale timeout.
     */
    EnrichmentService(List<Enricher> enrichers,
                             DraftMarkEvidenceCommandService draftCommandService,
                             DraftMarkEvidenceQueryService draftQueryService,
                             MarkEvidenceSubmissionQueryService submissionQueryService,
                             PlatformTransactionManager txManager,
                             long staleTimeoutMinutes,
                             Clock clock) {
        this.enrichers = enrichers;
        this.draftCommandService = draftCommandService;
        this.draftQueryService = draftQueryService;
        this.submissionQueryService = submissionQueryService;
        this.txManager = txManager;
        this.staleTimeoutMinutes = staleTimeoutMinutes;
        this.clock = clock;
    }

    public void enrichSubmission(Long submissionId) {
        enrichSubmissionInternal(submissionId);
    }

    /*
     * Internal synchronous implementation of enrichment flow. Extracted to allow deterministic
     * invocation from tests while the public method remains @Async for production use.
     */
    private void enrichSubmissionInternal(Long submissionId) {
        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            DraftMarkEvidence draft = draftQueryService.findBySubmissionId(submissionId)
                    .orElseGet(() -> draftCommandService.createIfMissingForSubmission(
                            DraftMarkEvidence.builder().submission(submission).build()
                    ));

            Long draftId = draft.getId();
            Instant now = Instant.now(clock);

            // --- PRE-CHECK (no lock) ---
            if (draft.getProcessingStatus() == ProcessingStatus.COMPLETED) {
                log.info("action=enrich skip reason=already_completed draftId={} submissionId={}", draftId, submissionId);
                return;
            }
            if (Boolean.FALSE.equals(draft.getActive())) {
                log.info("action=enrich skip reason=inactive draftId={} submissionId={}", draftId, submissionId);
                return;
            }
            if (draft.getProcessingStatus() == ProcessingStatus.IN_PROGRESS) {
                Instant last = draft.getLastModifiedAt();
                if (last != null && last.isAfter(now.minus(Duration.ofMinutes(staleTimeoutMinutes)))) {
                    log.info("action=enrich skip reason=concurrent_in_progress draftId={} submissionId={}", draftId, submissionId);
                    return;
                }
                log.warn("action=enrich warning=stale_in_progress draftId={} submissionId={}", draftId, submissionId);
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
                    log.warn("action=enrich enricher={} warning=failed draftId={} submissionId={} error={}",
                            enricher.getClass().getSimpleName(), draftId, submissionId, msg, e);
                    errors.add(msg);
                }
            }

            // --- FINALIZE DRAFT (REQUIRES_NEW) ---
            finalizeDraftTx(draftId, errors);

        }, () -> log.warn("action=enrich warning=submission_not_found submissionId={}", submissionId));
    }

    public void markInProgressTx(Long draftId) {
        executeInNewTransaction(draftId, draft -> {
            Instant now = Instant.now(clock);
            if (draft.getProcessingStatus() == ProcessingStatus.IN_PROGRESS) {
                Instant last = draft.getLastModifiedAt();
                if (last == null || last.isBefore(now.minus(Duration.ofMinutes(staleTimeoutMinutes)))) {
                    Long submissionId = getSubmissionId(draft);
                    log.warn("action=markInProgress warning=stale_in_progress draftId={} submissionId={}", draft.getId(), submissionId);
                    appendError(draft, "Stale IN_PROGRESS detected - attempting recovery");
                } else {
                    // Already in progress and not stale
                    return null;
                }
            }

            draft.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
            // single update after any modifications (including appended error)
            draftCommandService.update(draft);
            return null;
        });
    }

    private void appendError(DraftMarkEvidence draft, String error) {
        Long submissionId = getSubmissionId(draft);
        log.warn("action=appendError draftId={} submissionId={} error={}", draft.getId(), submissionId, error);
        String merged = mergeErrors(draft, java.util.List.of(error));
        draft.setProcessingError(merged);
    }

    /**
     * Merge existing draft processing error with a list of new errors, preserving line breaks.
     * Returns null when there are no existing or new errors.
     */
    private String mergeErrors(DraftMarkEvidence draft, List<String> newErrors) {
        String existing = draft.getProcessingError();
        String newJoined = (newErrors == null || newErrors.isEmpty()) ? null : String.join("\n", newErrors);

        if ((existing == null || existing.isEmpty()) && (newJoined == null || newJoined.isEmpty())) {
            return null;
        }
        if (existing == null || existing.isEmpty()) return newJoined;
        if (newJoined == null || newJoined.isEmpty()) return existing;
        return existing + "\n" + newJoined;
    }

    private Long getSubmissionId(DraftMarkEvidence draft) {
        return draft == null || draft.getSubmission() == null ? null : draft.getSubmission().getId();
    }

    public void finalizeDraftTx(Long draftId, List<String> collectedErrors) {
        executeInNewTransaction(draftId, draft -> {
            Long submissionId = draft.getSubmission() == null ? null : draft.getSubmission().getId();
            // Merge errors using helper that preserves line breaks
            String merged = mergeErrors(draft, collectedErrors);

            if (draft.getEmbedding() == null) {
                if (merged == null || merged.isEmpty()) merged = "Missing embedding after enrichment";
                draft.setProcessingStatus(ProcessingStatus.FAILED);
                draft.setProcessingError(merged);
                // Structured warning with merged errors for easier debugging
                log.warn("action=finalize outcome=FAILED draftId={} submissionId={} errors={}", draft.getId(), submissionId, merged);
            } else {
                draft.setProcessingStatus(ProcessingStatus.COMPLETED);
                draft.setProcessingError(null);
                log.info("action=finalize outcome=COMPLETED draftId={} submissionId={}", draft.getId(), submissionId);
            }

            draftCommandService.update(draft);
            return null;
        });
    }

    /**
     * Execute an operation on a draft inside a new transaction. The draft is loaded with a FOR UPDATE
     * lock to prevent concurrent modifications.
     */
    private <T> T executeInNewTransaction(Long draftId, Function<DraftMarkEvidence, T> action) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tt.execute(status -> {
            DraftMarkEvidence draft = draftQueryService.findByIdForUpdate(draftId)
                    .orElseThrow(() -> new IllegalStateException("Draft with id " + draftId + " not found"));
            return action.apply(draft);
        });
    }
}
