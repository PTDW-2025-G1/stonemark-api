package pt.estga.processing.services.enrichers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.services.MarkCreationService;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;

/**
 * When no suggested occurrence is available, try to suggest a Mark (and
 * optionally an occurrence) based on available hints. This enricher keeps
 * behavior conservative: it will create a new Mark/Occurrence only when there
 * is a reasonable hint (for example, an AI-provided title hint or embedding)
 * and only if the draft has no existing suggestion.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuggestedMarkEnricher implements Enricher {

    private final DraftMarkEvidenceQueryService draftQueryService;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final MarkCreationService markCreationService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrich(Long draftId) {
        draftQueryService.findById(draftId).ifPresentOrElse(draft -> {
            if (draft.getSuggestedOccurrence() != null || draft.getSuggestedMark() != null) {
                log.debug("Draft {} already has suggestions — skipping suggested mark enrichment", draftId);
                return;
            }

            // Use embedding or a placeholder title hint from aiMetadataJson to create a mark if helpful.
            String titleHint = null;
            if (draft.getAiMetadataJson() != null && draft.getAiMetadataJson().contains("title")) {
                // very simple heuristic — in future parse JSON properly
                titleHint = "AI suggested";
            }

            if (draft.getEmbedding() == null && titleHint == null) {
                log.debug("Draft {} has no embedding or title hint — skipping mark suggestion", draftId);
                return;
            }

            // Create a new Mark/Occurrence pair and record suggestedMark and suggestedOccurrence.
            try {
                MarkOccurrence created = markCreationService.createFromDraft(titleHint, draft.getEmbedding());
                if (created != null) {
                    draft.setSuggestedOccurrence(created);
                    draft.setSuggestedMark(created.getMark());
                    draftCommandService.update(draft);
                    log.info("Created suggested Mark/Occurrence for draft {}: occurrenceId={}", draftId, created.getId());
                }
            } catch (Exception e) {
                log.warn("SuggestedMarkEnricher failed for draft {}", draftId, e);
            }
        }, () -> log.warn("Draft with id {} not found for suggested mark enricher", draftId));
    }
}
