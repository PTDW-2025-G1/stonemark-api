package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.entities.DraftMarkEvidence;
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
    private final pt.estga.processing.services.draft.DraftMarkEvidenceCommandService draftCommandService;
    private final pt.estga.processing.services.draft.DraftMarkEvidenceQueryService draftQueryService;
    private final pt.estga.intake.services.MarkEvidenceSubmissionQueryService submissionQueryService;

    /**
     * Enrich the submission by delegating to configured enrichers.
     * The method is executed with REQUIRES_NEW to ensure it runs after the
     * origin transaction commits. Individual enrichers also run in their own
     * REQUIRES_NEW transactions.
     *
     * @param submissionId id of the submission to enrich
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrichSubmission(Long submissionId) {
        // Ensure there is a draft linked to the submission and obtain its id.
        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            DraftMarkEvidence draft = draftQueryService.findBySubmissionId(submissionId)
                    .orElseGet(() -> draftCommandService.createIfMissingForSubmission(
                            DraftMarkEvidence.builder().submission(submission).build()
                    ));

            Long draftId = draft.getId();

            for (Enricher enricher : enrichers) {
                try {
                    enricher.enrich(draftId);
                } catch (Exception e) {
                    log.warn("Enricher {} failed for draft {} - continuing with next enricher", enricher.getClass().getSimpleName(), draftId, e);
                }
            }
        }, () -> log.warn("Submission with id {} not found - skipping enrichment", submissionId));
    }
}
