package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;
    private final MarkEvidenceReviewRepository markEvidenceReviewRepository;
    private final MarkRepository markRepository;
    private final ReviewExecutor executor;

    @Value("${review.allow-empty-review:false}")
    private boolean allowEmptyReview;

    @Value("${review.new-mark.max-suggestion-confidence:0.5}")
    private double newMarkMaxSuggestionConfidence;

    @Transactional
    public MarkEvidenceReview acceptAsNew(Long submissionId, String newMarkTitle, String comment) {
        var overview = processingRepository.findOverviewBySubmissionId(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Processing not found"));

        Double maxConf = suggestionRepository.findMaxConfidenceByProcessingId(overview.getId());
        if (maxConf != null && maxConf >= newMarkMaxSuggestionConfidence) {
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

    @Transactional
    public MarkEvidenceReview performReview(Long submissionId, ReviewType type, DiscoveryContext ctx, String comment) {
        var submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

        var overview = processingRepository.findOverviewBySubmissionId(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " not processed"));

        validateState(submissionId, overview.getStatus(), suggestionRepository.countByProcessingId(overview.getId()), type);

        ResolutionResult resolution = resolve(type, ctx);

        ReviewDecision decision = (type == ReviewType.REJECTION) ? ReviewDecision.REJECTED : ReviewDecision.APPROVED;
        try {
            return executor.execute(submission, decision, comment, resolution, overview.getId());
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
    public ReviewDecision getReviewStatus(Long submissionId) {
        return markEvidenceReviewRepository.findBySubmissionId(submissionId)
                .map(MarkEvidenceReview::getDecision)
                .orElse(null);
    }

    private ResolutionResult resolve(ReviewType type, DiscoveryContext ctx) {
        return switch (type) {
            case MATCH -> {
                Mark mark = ctx.existingMarkId() != null
                        ? markRepository.findById(ctx.existingMarkId()).orElseThrow()
                        : null;
                yield new ResolutionResult(mark);
            }
            case DISCOVERY -> {
                Mark mark = null;
                if (ctx.existingMarkId() != null) {
                    mark = markRepository.findById(ctx.existingMarkId()).orElseThrow();
                } else if (ctx.markTitle() != null) {
                    mark = markRepository.save(Mark.builder().title(ctx.markTitle()).build());
                }
                yield new ResolutionResult(mark);
            }
            case REJECTION -> new ResolutionResult(null);
        };
    }

    private void validateState(Long submissionId, ProcessingStatus status, long count, ReviewType reviewType) {
        if (status != ProcessingStatus.COMPLETED && status != ProcessingStatus.REVIEW_PENDING) {
            throw new IllegalStateException("Processing not ready.");
        }
        if (reviewType != ReviewType.DISCOVERY && !allowEmptyReview && count == 0) {
            throw new IllegalStateException("No suggestions available to review.");
        }
        if (markEvidenceReviewRepository.existsBySubmissionId(submissionId)) {
            throw new IllegalStateException("Submission already reviewed.");
        }
    }
}
