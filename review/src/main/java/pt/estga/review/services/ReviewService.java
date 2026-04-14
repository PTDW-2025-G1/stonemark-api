package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.services.markevidenceprocessing.MarkEvidenceProcessingQueryService;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.services.markevidencereview.MarkEvidenceReviewQueryService;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import pt.estga.review.models.ResolutionResult;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.processors.ReviewProcessor;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

 	private final MarkEvidenceSubmissionQueryService submissionQueryService;
 	private final MarkEvidenceProcessingQueryService processingQueryService;
	private final MarkSuggestionQueryService suggestionQueryService;
  	private final MarkEvidenceReviewQueryService markEvidenceReviewQueryService;
	private final List<ReviewProcessor> processors;
	private final ReviewExecutor executor;

	@Value("${review.allow-empty-review:false}")
	private boolean allowEmptyReview;

	@Value("${review.new-mark.max-suggestion-confidence:0.5}")
	private double newMarkMaxSuggestionConfidence;

	/**
	 * Accept the submission with a new mark (not from suggestions). Creates a review with APPROVED decision.
	 * Enforces: processing must be COMPLETED; review must not already exist; if accepted, mark submission as PROCESSED.
	 */
	@Transactional
	public MarkEvidenceReview acceptAsNew(Long submissionId, String newMarTitle, String comment) {
		// Fetch overview to check confidence
		var overview = processingQueryService.findOverviewBySubmissionId(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Processing not found"));

		// Guard: Don't allow "New Mark" if the AI found a very strong match
		Double maxConf = suggestionQueryService.findMaxConfidenceByProcessingId(overview.getId());
		if (maxConf != null && maxConf >= newMarkMaxSuggestionConfidence) {
			throw new IllegalStateException("Confident suggestions exist. You must review existing marks.");
		}

		DiscoveryContext ctx = new DiscoveryContext(newMarTitle, null, null, null, null);
		return performReview(submissionId, ReviewType.DISCOVERY, ctx, comment);
	}

	/**
	 * Accept a suggested mark for the given submission.
	 * Enforces: processing must be COMPLETED; review must not already exist; if accepted, mark submission as PROCESSED.
	 */
	@Transactional
	public MarkEvidenceReview acceptSuggestion(Long submissionId, Long markId, String comment) {
		DiscoveryContext ctx = new DiscoveryContext(null, markId, null, null, null);
		return performReview(submissionId, ReviewType.MATCH, ctx, comment);
	}

	/**
	 * Reject all suggestions for a submission. Creates a review with REJECTED decision.
	 * Enforces: processing must be COMPLETED; review must not already exist.
	 */
	@Transactional
	public MarkEvidenceReview rejectAll(Long submissionId, String comment) {
		return performReview(submissionId, ReviewType.REJECTION, null, comment);
	}

	@Transactional
	public MarkEvidenceReview performReview(Long submissionId, ReviewType type, DiscoveryContext ctx, String comment) {
		// 1. Fetch data
		var submission = submissionQueryService.findById(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

		var overview = processingQueryService.findOverviewBySubmissionId(submissionId)
				.orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " not processed"));

		// 2. Policy validation
		validateState(submissionId, overview.getStatus(), suggestionQueryService.countByProcessingId(overview.getId()), type);

		// 3. Resolve entities
		ReviewProcessor processor = processors.stream()
				.filter(p -> p.getSupportedType() == type)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No processor for review type: " + type));

		ResolutionResult resolution = processor.resolve(submissionId, ctx);

		// 4. Decide and execute
		ReviewDecision decision = (type == ReviewType.REJECTION) ? ReviewDecision.REJECTED : ReviewDecision.APPROVED;
		return executor.execute(submission, decision, comment, resolution, overview.getId());
	}

	/**
	 * Returns the current review decision for a submission, if present.
	 */
	@Transactional(readOnly = true)
	public ReviewDecision getReviewStatus(Long submissionId) {
		return markEvidenceReviewQueryService.findBySubmissionId(submissionId)
				.map(MarkEvidenceReview::getDecision)
				.orElse(null);
	}

	private void validateState(Long submissionId, ProcessingStatus status, long count, ReviewType reviewType) {
		if (status != ProcessingStatus.COMPLETED && status != ProcessingStatus.REVIEW_PENDING) {
			throw new IllegalStateException("Processing not ready.");
		}
		// If this ISN'T a discovery/new mark, we might require suggestions to exist
		if (reviewType != ReviewType.DISCOVERY && !allowEmptyReview && count == 0) {
			throw new IllegalStateException("No suggestions available to review.");
		}
		if (markEvidenceReviewQueryService.existsBySubmissionId(submissionId)) {
			throw new IllegalStateException("Submission already reviewed.");
		}
	}
}
