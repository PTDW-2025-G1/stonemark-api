package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
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
public class ReviewDecisionService {

    private final DraftMarkEvidenceQueryService draftQueryService;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final DraftReviewQueryService draftReviewQueryService;
    private final DraftReviewCommandService draftReviewCommandService;
    private final MarkEvidenceCommandService evidenceCommandService;
    private final MarkCreationService markCreationService;

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
        DraftMarkEvidence draft = draftQueryService.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("Draft not found with id: " + draftId));
        // Idempotency: if a review record already exists for this draft, skip processing
        if (draftReviewQueryService.existsByDraftId(draftId)) return;

        // Persist the decision as a dedicated draft review record
        DraftReview review = DraftReview.builder()
                .draft(draft)
                .decision(decision)
                .reviewedAt(Instant.now())
                .build();

        draftReviewCommandService.create(review);

        if (decision == ReviewDecision.REJECTED) {
            draft.setActive(false);
            draftCommandService.update(draft);
            return;
        }

        if (decision == ReviewDecision.FLAGGED) {
            // Do not deactivate the draft when flagged; persist any changes
            draftCommandService.update(draft);
            return;
        }

        // APPROVED path
        MarkOccurrence targetOccurrence;
        if (draft.getSuggestedOccurrence() != null) {
            targetOccurrence = draft.getSuggestedOccurrence();
        } else {
            String titleHint = draft.getSubmission() != null && draft.getSubmission().getUserNotes() != null
                    ? draft.getSubmission().getUserNotes() : null;

            targetOccurrence = markCreationService.createFromDraft(titleHint, draft.getEmbedding());
        }

        // Deduplication: check if similar evidence already exists for the occurrence
        double threshold = 0.95; // configurable threshold; tune as needed
        boolean exists = evidenceCommandService.existsSimilar(draft.getEmbedding(), targetOccurrence, threshold);
        if (exists) {
            draft.setActive(false);
            draftCommandService.update(draft);
            return;
        }

        MarkEvidence evidence = MarkEvidence.builder()
                .occurrence(targetOccurrence)
                .embedding(draft.getEmbedding())
                .build();

        evidenceCommandService.create(evidence);

        draft.setActive(false);
        draftCommandService.update(draft);
    }
}
