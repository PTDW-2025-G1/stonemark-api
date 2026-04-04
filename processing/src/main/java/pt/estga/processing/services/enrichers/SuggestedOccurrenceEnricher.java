package pt.estga.processing.services.enrichers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.services.evidence.MarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;

/**
 * Tries to find an existing MarkOccurrence similar to the draft by embedding.
 * If found, sets draft.suggestedOccurrence. Uses conservative queries and
 * leaves the draft untouched when no reliable suggestion can be made.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuggestedOccurrenceEnricher implements Enricher {

    private final DraftMarkEvidenceQueryService draftQueryService;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final MarkEvidenceCommandService evidenceCommandService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrich(Long draftId) {
        draftQueryService.findById(draftId).ifPresentOrElse(draft -> {
            if (draft.getEmbedding() == null) {
                log.debug("Draft {} has no embedding — skipping suggested occurrence enrichment", draftId);
                return;
            }

            // Conservative check: ask the evidence service whether similar evidence
            // exists in any occurrence. If so, do not attempt to guess the occurrence id
            // unless a dedicated finder is available. For now set suggestedOccurrence to null
            // only when we can confidently identify it. This implementation will only flag
            // that similar evidence exists and leave suggestion to a future, more capable enricher.
            boolean existsSimilar = evidenceCommandService.existsSimilar(draft.getEmbedding(), null, 0.95);
            if (existsSimilar) {
                log.info("Similar evidence detected for draft {} — a suggested occurrence may exist but not resolved by this enricher", draftId);
                // Do not overwrite an existing suggestion; just record AI flag if not present
                if (draft.getAiFlagged() == null) {
                    draft.setAiFlagged(true);
                }
                draftCommandService.update(draft);
            } else {
                log.debug("No similar evidence found for draft {}", draftId);
            }
        }, () -> log.warn("Draft with id {} not found for suggested occurrence enricher", draftId));
    }
}
