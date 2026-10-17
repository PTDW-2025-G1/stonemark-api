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
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
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
    private final ReviewExecutor executor;
    private final MeterRegistry meterRegistry;

    @Value("${review.allow-empty-review:false}")
    private boolean allowEmptyReview;

    @Value("${review.new-mark.max-suggestion-confidence:0.5}")
    private double newMarkMaxSuggestionConfidence;

    @Transactional
    public MarkEvidenceReview acceptAsNew(Long submissionId, String newMarkTitle, String comment) {
        intakeApi.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

        Double maxConfidence = processingApi.findOverview(submissionId)
                .map(overview -> processingApi.maxSuggestionConfidence(overview.processingId()))
                .orElse(null);
        if (maxConfidence != null && maxConfidence >= newMarkMaxSuggestionConfidence) {
            throw new IllegalStateException("Confident suggestions exist. You must review existing marks.");
        }

        DiscoveryContext ctx = new DiscoveryContext(newMarkTitle, null, null, null, null);
        return performReview(submissionId, ReviewType.DISCOVERY, ctx, comment);
    }

    @Transactional
    public MarkEvidenceReview acceptSuggestion(Long submissionId, Long markId, String comment) {
        DiscoveryContext ctx = new DiscoveryContext(null, markId, null, null, null);
        return performReview(submissionId, ReviewType.MATCH, ctx, comment);
    }

    @Transactional
    public MarkEvidenceReview rejectAll(Long submissionId, String comment) {
        return performReview(submissionId, ReviewType.REJECTION, null, comment);
    }

    /**
     * Records a moderation decision for a submission and persists it as a label.
     *
     * <p>Processing is optional: a submission that was never processed can still be
     * moderated manually. When a processing record exists, its status and suggestions
     * gate the decision.
     */
    @Transactional
    public MarkEvidenceReview performReview(Long submissionId, ReviewType type, DiscoveryContext ctx, String comment) {
        SubmissionRef submission = intakeApi.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

        ProcessingOverview overview = processingApi.findOverview(submissionId).orElse(null);
        boolean processed = overview != null;
        long suggestionCount = processed ? processingApi.countSuggestions(overview.processingId()) : 0L;

        if (processed && overview.status() != ProcessingStatus.COMPLETED) {
            throw new IllegalStateException("Processing not ready.");
        }
        if (processed && type != ReviewType.DISCOVERY && !allowEmptyReview && suggestionCount == 0) {
            throw new IllegalStateException("No suggestions available to review.");
        }
        if (markEvidenceReviewRepository.existsBySubmissionId(submissionId)) {
            throw new IllegalStateException("Submission already reviewed.");
        }

        ResolutionResult resolution = resolve(type, ctx);
        ReviewDecision decision = (type == ReviewType.REJECTION) ? ReviewDecision.REJECTED : ReviewDecision.APPROVED;

        try {
            MarkEvidenceReview review = executor.execute(submissionId, decision, comment, resolution);
            applyReviewSideEffects(submission, decision, resolution, processed);
            return review;
        } catch (DataIntegrityViolationException dive) {
            throw new IllegalStateException("Submission " + submissionId + " already reviewed", dive);
        }
    }

    @Transactional(readOnly = true)
    public List<MarkSuggestionDto> getSuggestions(Long submissionId) {
        ProcessingOverview overview = processingApi.findOverview(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestions not found"));
        return processingApi.findSuggestions(overview.processingId());
    }

    @Transactional(readOnly = true)
    public Optional<ReviewDecision> getReviewStatus(Long submissionId) {
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

    private ResolutionResult resolve(ReviewType type, DiscoveryContext ctx) {
        return switch (type) {
            case MATCH -> {
                MarkDto mark = ctx.existingMarkId() != null
                        ? markQueryService.findMarkById(ctx.existingMarkId()).orElseThrow()
                        : null;
                yield new ResolutionResult(mark);
            }
            case DISCOVERY -> {
                MarkDto mark = null;
                if (ctx.existingMarkId() != null) {
                    mark = markQueryService.findMarkById(ctx.existingMarkId()).orElseThrow();
                } else if (ctx.markTitle() != null) {
                    mark = markQueryService.createMark(ctx.markTitle());
                }
                yield new ResolutionResult(mark);
            }
            case REJECTION -> new ResolutionResult(null);
        };
    }

    private void applyReviewSideEffects(SubmissionRef submission, ReviewDecision decision,
                                        ResolutionResult resolution, boolean processed) {
        Long submissionId = submission.id();

        if (decision == ReviewDecision.APPROVED) {
            intakeApi.markProcessed(submissionId);
        } else {
            intakeApi.markRejected(submissionId);
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
            linkEvidenceToMark(submissionId, submission.originalMediaFileId(), resolution.mark().id());
        }
    }

    /**
     * Links the submission's photo to a mark occurrence.
     *
     * <p>Deliberately not wrapped in a catch: this runs inside the review
     * transaction, so a linking failure rolls the whole decision back and a
     * submission can never be approved without its evidence being linked. The
     * embedding is optional and is absent when the submission was not processed.
     */
    private void linkEvidenceToMark(Long submissionId, UUID mediaFileId, Long markId) {
        if (mediaFileId == null) {
            throw new IllegalStateException("Submission " + submissionId + " has no media to link");
        }
        MarkOccurrenceDto occurrence = markQueryService.findOrCreateOccurrence(markId, true);
        float[] embedding = parseEmbedding(processingApi.findEmbeddingText(submissionId));
        markQueryService.linkFileToOccurrence(mediaFileId, occurrence.id(), embedding);
        log.info("Linked evidence file {} to occurrence {} (submission {})",
                mediaFileId, occurrence.id(), submissionId);
    }

    private static float[] parseEmbedding(Optional<String> text) {
        return text.map(t -> {
            String trimmed = t.trim();
            if (trimmed.isEmpty() || "[]".equals(trimmed)) return null;
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            String[] parts = trimmed.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        }).orElse(null);
    }
}
