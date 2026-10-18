package pt.estga.review.services;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.intake.api.SubmissionRef;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.processing.api.ProcessingOverview;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.core.models.EmbeddingVector;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final IntakeSubmissionApi intakeApi;
    private final ProcessingReviewApi processingApi;
    private final MarkEvidenceReviewRepository markEvidenceReviewRepository;
    private final MarkQueryService markQueryService;
    private final MarkCreationSuggestionService markCreationSuggestionService;
    private final ReviewExecutor executor;
    private final MeterRegistry meterRegistry;

    @Value("${review.allow-empty-review:false}")
    private boolean allowEmptyReview;

    /**
     * Accepts a submission as an existing mark.
     */
    @Transactional
    public MarkEvidenceReview acceptSuggestion(UUID submissionId, UUID markId, String comment) {
        return performReview(submissionId, ReviewType.MATCH, markId, comment);
    }

    /**
     * Records that no existing mark matches the submission. The submission is kept
     * as a candidate for a new mark; no mark is provisioned here. Once enough similar
     * candidates accumulate, a mark-creation suggestion is raised for moderators.
     */
    @Transactional
    public MarkEvidenceReview noMatch(UUID submissionId, String comment) {
        MarkEvidenceReview review = performReview(submissionId, ReviewType.NO_MATCH, null, comment);
        markCreationSuggestionService.recompute();
        return review;
    }

    @Transactional
    public MarkEvidenceReview rejectAll(UUID submissionId, String comment) {
        return performReview(submissionId, ReviewType.REJECTION, null, comment);
    }

    /**
     * Records an automated approval. Used by auto-approval when the system is
     * confident and unambiguous about a match to an existing mark; it never creates
     * a mark.
     */
    @Transactional
    public MarkEvidenceReview autoApprove(UUID submissionId, UUID markId) {
        return performReview(submissionId, ReviewType.MATCH, markId,
                "Auto-approved: confident, unambiguous match");
    }

    /**
     * Records a moderation decision for a submission and persists it as a label.
     *
     * <p>Processing is optional: a submission that was never processed can still be
     * moderated manually. When a processing record exists, its status and suggestions
     * gate the decision.
     */
    @Transactional
    public MarkEvidenceReview performReview(UUID submissionId, ReviewType type, UUID existingMarkId, String comment) {
        SubmissionRef submission = intakeApi.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

        ProcessingOverview overview = processingApi.findOverview(submissionId).orElse(null);
        boolean processed = overview != null;
        long suggestionCount = processed ? processingApi.countSuggestions(overview.processingId()) : 0L;

        // A submission whose processing did not complete can still be rejected, so a
        // broken record never blocks the queue; every other action waits for it.
        if (processed && type != ReviewType.REJECTION && overview.status() != ProcessingStatus.COMPLETED) {
            throw new IllegalStateException("Processing not ready.");
        }
        if (processed && type == ReviewType.MATCH && !allowEmptyReview && suggestionCount == 0) {
            throw new IllegalStateException("No suggestions available to review.");
        }
        if (markEvidenceReviewRepository.existsBySubmissionId(submissionId)) {
            throw new IllegalStateException("Submission already reviewed.");
        }

        ResolutionResult resolution = resolve(type, existingMarkId);
        ReviewDecision decision = switch (type) {
            case MATCH -> ReviewDecision.APPROVED;
            case NO_MATCH -> ReviewDecision.NO_MATCH;
            case REJECTION -> ReviewDecision.REJECTED;
        };

        try {
            MarkEvidenceReview review = executor.execute(submissionId, decision, comment, resolution);
            applyReviewSideEffects(submission, decision, resolution, processed);
            return review;
        } catch (DataIntegrityViolationException dive) {
            throw new IllegalStateException("Submission " + submissionId + " already reviewed", dive);
        }
    }

    @Transactional(readOnly = true)
    public List<MarkSuggestionDto> getSuggestions(UUID submissionId) {
        ProcessingOverview overview = processingApi.findOverview(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestions not found"));
        return processingApi.findSuggestions(overview.processingId());
    }

    @Transactional(readOnly = true)
    public Optional<ReviewDecision> getReviewStatus(UUID submissionId) {
        return markEvidenceReviewRepository.findBySubmissionId(submissionId)
                .map(MarkEvidenceReview::getDecision);
    }

    /**
     * Lists persisted moderation decisions (the training labels), newest first by default.
     */
    @Transactional(readOnly = true)
    public Page<MarkEvidenceReview> listReviews(Pageable pageable) {
        return markEvidenceReviewRepository.findAll(pageable);
    }

    private ResolutionResult resolve(ReviewType type, UUID existingMarkId) {
        return switch (type) {
            case MATCH -> {
                if (existingMarkId == null) {
                    throw new IllegalArgumentException("A mark id is required to accept a suggestion");
                }
                MarkDto mark = markQueryService.findMarkById(existingMarkId)
                        .orElseThrow(() -> new ResourceNotFoundException("Mark " + existingMarkId + " not found"));
                yield new ResolutionResult(mark);
            }
            case NO_MATCH, REJECTION -> new ResolutionResult(null);
        };
    }

    private void applyReviewSideEffects(SubmissionRef submission, ReviewDecision decision,
                                        ResolutionResult resolution, boolean processed) {
        UUID submissionId = submission.id();

        if (decision == ReviewDecision.REJECTED) {
            intakeApi.markRejected(submissionId);
        } else {
            intakeApi.markProcessed(submissionId);
        }
        try {
            meterRegistry.counter("review.applied", "decision", decision.name()).increment();
        } catch (Exception ex) {
            log.debug("Failed to increment review.applied metric: {}", ex.getMessage());
        }

        if (processed) {
            processingApi.markReviewed(submissionId);
        }

        if (decision == ReviewDecision.APPROVED && resolution != null && resolution.mark() != null) {
            linkEvidenceToMark(submission, resolution.mark());
        }
    }

    /**
     * Links the submission's photo to a mark occurrence, carrying the submission's
     * coordinates onto the occurrence when present. When the accepting mark is already
     * published, the occurrence is published too, so the approval has a public effect.
     *
     * <p>Deliberately not wrapped in a catch: this runs inside the review
     * transaction, so a linking failure rolls the whole decision back and a
     * submission can never be approved without its evidence being linked. The
     * embedding is optional and is absent when the submission was not processed.
     */
    private void linkEvidenceToMark(SubmissionRef submission, MarkDto mark) {
        UUID mediaFileId = submission.originalMediaFileId();
        if (mediaFileId == null) {
            throw new IllegalStateException("Submission " + submission.id() + " has no media to link");
        }
        MarkOccurrenceDto occurrence = markQueryService.findOrCreateOccurrence(
                mark.id(), null, submission.latitude(), submission.longitude());
        if (mark.status() == PublicationStatus.PUBLISHED && occurrence.status() != PublicationStatus.PUBLISHED) {
            markQueryService.publishOccurrence(occurrence.id());
        }
        EmbeddingVector embedding = processingApi.findEmbedding(submission.id()).orElse(null);
        markQueryService.linkFileToOccurrence(mediaFileId, occurrence.id(), submission.id(), embedding);
        log.info("Linked evidence file {} to occurrence {} (submission {})",
                mediaFileId, occurrence.id(), submission.id());
    }
}
