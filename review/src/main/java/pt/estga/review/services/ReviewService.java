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
import pt.estga.shared.web.exceptions.ResourceNotFoundException;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;
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
        submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

        Double maxConfidence = processingRepository.findOverviewBySubmissionId(submissionId)
                .map(overview -> suggestionRepository.findMaxConfidenceByProcessingId(overview.getId()))
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
     * <p>Processing is optional. A submission that was never processed by the AI
     * pipeline can still be moderated manually — that is how the labelled corpus the
     * model will later learn from is built. When a processing record exists, its
     * status and suggestions gate the decision as before.
     */
    @Transactional
    public MarkEvidenceReview performReview(Long submissionId, ReviewType type, DiscoveryContext ctx, String comment) {
        var submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

        var overview = processingRepository.findOverviewBySubmissionId(submissionId).orElse(null);
        boolean processed = overview != null;
        long suggestionCount = processed ? suggestionRepository.countByProcessingId(overview.getId()) : 0L;

        if (processed && overview.getStatus() != ProcessingStatus.COMPLETED) {
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
            MarkEvidenceReview review = executor.execute(
                    submission, decision, comment, resolution, processed ? overview.getId() : null);
            applyReviewSideEffects(submission, decision, resolution, processed);
            return review;
        } catch (DataIntegrityViolationException dive) {
            throw new IllegalStateException("Submission " + submissionId + " already reviewed", dive);
        }
    }

    @Transactional(readOnly = true)
    public List<MarkSuggestionDto> getSuggestions(Long submissionId) {
        return processingRepository.findBySubmissionId(submissionId)
                .map(p -> suggestionRepository.findByProcessingId(p.getId())
                        .stream()
                        .map(MarkSuggestionMapper::toDto)
                        .toList())
                .orElseThrow(() -> new ResourceNotFoundException("Suggestions not found"));
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

    private void applyReviewSideEffects(MarkEvidenceSubmission submission, ReviewDecision decision,
                                        ResolutionResult resolution, boolean processed) {
        Long submissionId = submission.getId();
        SubmissionStatus targetStatus = decision == ReviewDecision.APPROVED
                ? SubmissionStatus.PROCESSED : SubmissionStatus.REJECTED;

        if (submission.getStatus() != targetStatus) {
            if (targetStatus == SubmissionStatus.PROCESSED) submission.markProcessed();
            else submission.markRejected();
            submissionRepository.save(submission);
            try {
                meterRegistry.counter("review.applied", "decision", decision.name()).increment();
            } catch (Exception ex) {
                log.debug("Failed to increment review.applied metric: {}", ex.getMessage());
            }
        }

        if (processed) {
            try {
                processingRepository.updateStatusBySubmissionId(submissionId, ProcessingStatus.REVIEWED);
            } catch (Exception ex) {
                log.error("Failed to mark processing REVIEWED for submission {}", submissionId, ex);
            }
        }

        if (decision == ReviewDecision.APPROVED && resolution != null && resolution.mark() != null) {
            linkEvidenceToMark(submissionId, submission.getOriginalMediaFileId(), resolution.mark().id());
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
        float[] embedding = parseEmbedding(processingRepository.findEmbeddingTextBySubmissionId(submissionId));
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
