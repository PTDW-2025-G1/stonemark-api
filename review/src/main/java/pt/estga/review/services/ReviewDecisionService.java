package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.review.entities.DraftReview;
import pt.estga.review.services.draft.DraftReviewCommandService;
import pt.estga.review.services.draft.DraftReviewQueryService;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.mark.services.evidence.MarkEvidenceCommandService;
import pt.estga.mark.services.MarkCreationService;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.time.Instant;
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewDecisionService {

    private final DraftMarkEvidenceQueryService draftQueryService;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final DraftReviewQueryService draftReviewQueryService;
    private final DraftReviewCommandService draftReviewCommandService;
    private final MarkEvidenceCommandService evidenceCommandService;
    private final MarkCreationService markCreationService;

    @Value("${review.evidence.similarity-threshold:0.95}")
    private double similarityThreshold;

    @Transactional
    public void applyDecision(Long reviewId, ReviewDecision decision) {
        // Load review entity -- repository not yet implemented; look up draft by id instead
        throw new UnsupportedOperationException("Review repository lookup not implemented yet");
    }

    /**
     * Apply decision for a given draft id. This is the main code path used by processing.
     */
    @Transactional
    public void applyDecisionToDraft(Long draftId, ReviewDecision decision) {
        applyDecisionToDraft(draftId, decision, null, null);
    }

    /**
     * Apply decision for a given draft id, optionally storing reviewer info and notes.
     */
    @Transactional
    public void applyDecisionToDraft(Long draftId, ReviewDecision decision, Long reviewerId, String reviewerNotes) {
        // Load the draft with a pessimistic lock to avoid concurrent reviewers processing same draft
        DraftMarkEvidence draft = draftQueryService.findByIdForUpdate(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("Draft not found with id: " + draftId));

        log.info("Applying review decision {} to draft id={}", decision, draftId);

        // Skip if draft already inactive
        if (Boolean.FALSE.equals(draft.getActive())) {
            log.info("Skipping draft id={} because it is inactive", draftId);
            return;
        }

        // Idempotency: if a review record already exists for this draft, skip processing
        if (draftReviewQueryService.existsByDraftId(draftId)) {
            log.info("Skipping draft id={} because a DraftReview already exists", draftId);
            return;
        }

        if (draft.getEmbedding() == null) {
            throw new IllegalStateException("Draft embedding is missing");
        }

        // Persist the decision as a dedicated draft review record
        DraftReview.DraftReviewBuilder reviewBuilder = DraftReview.builder()
                .draft(draft)
                .decision(decision)
                .reviewedAt(Instant.now())
                .reviewerId(reviewerId)
                .reviewerNotes(reviewerNotes);

        DraftReview review = reviewBuilder.build();
        draftReviewCommandService.create(review);

        if (decision == ReviewDecision.REJECTED) {
            log.info("Draft id={} rejected; deactivating draft", draftId);
            deactivateDraft(draft);
            return;
        }

        if (decision == ReviewDecision.FLAGGED) {
            log.info("Draft id={} flagged; leaving draft active for further inspection", draftId);
            // Do not deactivate the draft when flagged; persist any changes
            draftCommandService.update(draft);
            return;
        }

        // APPROVED path
        MarkOccurrence targetOccurrence;
        if (draft.getSuggestedOccurrence() != null) {
            targetOccurrence = draft.getSuggestedOccurrence();
            log.info("Using suggested occurrence id={} for draft id={}", targetOccurrence.getId(), draftId);
        } else {
            String titleHint = draft.getSubmission() != null && draft.getSubmission().getUserNotes() != null
                    ? draft.getSubmission().getUserNotes() : null;

            targetOccurrence = markCreationService.createFromDraft(titleHint, draft.getEmbedding());
            log.info("Created new mark/occurrence id={} for draft id={}", targetOccurrence.getId(), draftId);
        }

        // Deduplication: check if similar evidence already exists for the occurrence
        boolean exists = evidenceCommandService.existsSimilar(draft.getEmbedding(), targetOccurrence, similarityThreshold);
        if (exists) {
            log.info("Similar evidence detected for draft id={} and occurrence id={}; skipping creation", draftId, targetOccurrence.getId());
            deactivateDraft(draft);
            return;
        }

        MarkEvidence evidence = MarkEvidence.builder()
                .occurrence(targetOccurrence)
                .embedding(draft.getEmbedding())
                .build();

        evidenceCommandService.create(evidence);
        log.info("Created MarkEvidence for draft id={} linked to occurrence id={}", draftId, targetOccurrence.getId());

        deactivateDraft(draft);
    }

    private void deactivateDraft(DraftMarkEvidence draft) {
        draft.setActive(false);
        draftCommandService.update(draft);
    }
}
