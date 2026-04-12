package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.services.markevidenceprocessing.MarkEvidenceProcessingQueryService;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;
import pt.estga.mark.services.mark.MarkCommandService;
import pt.estga.mark.services.mark.MarkQueryService;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import pt.estga.shared.utils.SecurityUtils;
import pt.estga.user.services.UserQueryService;
import pt.estga.shared.events.AfterCommitEventPublisher;
import pt.estga.review.events.ReviewCompletedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

 	private final MarkEvidenceSubmissionQueryService submissionQueryService;
 	private final MarkEvidenceProcessingQueryService processingQueryService;
 	private final MarkSuggestionQueryService suggestionQueryService;
 	private final MarkCommandService markCommandService;
 	private final MarkQueryService markQueryService;
	private final MarkEvidenceReviewRepository reviewRepository;
	private final UserQueryService userQueryService;
	private final AfterCommitEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

	@Value("${review.allow-non-suggested:false}")
	private boolean allowNonSuggested;

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
		// 1. Fetch overview to check confidence
		var overview = processingQueryService.findOverviewBySubmissionId(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Processing not found"));

		// Guard: Don't allow "New Mark" if the AI found a very strong match
		Double maxConf = suggestionQueryService.findMaxConfidenceByProcessingId(overview.getId());
		if (maxConf != null && maxConf >= newMarkMaxSuggestionConfidence) {
			throw new IllegalStateException("Confident suggestions exist. You must review existing marks.");
		}

		// 2. Create the Mark
		Mark newMark = markCommandService.create(Mark.builder().title(newMarTitle).build());

		// 3. Process. We pass 'true' to skip the "must have suggestions" check.
		return processReview(submissionId, ReviewDecision.APPROVED, newMark, comment, null, true);
	}

	/**
	 * Accept a suggested mark for the given submission.
	 * Enforces: processing must be COMPLETED; review must not already exist; if accepted, mark submission as PROCESSED.
	 */
	@Transactional
	public MarkEvidenceReview acceptSuggestion(Long submissionId, Long markId, String comment) {
		Mark mark = markQueryService.findById(markId)
				.orElseThrow(() -> new ResourceNotFoundException("Mark " + markId + " not found"));

		return processReview(submissionId, ReviewDecision.APPROVED, mark, comment, (procId) -> {
			if (!suggestionQueryService.existsByProcessingIdAndMarkId(procId, markId) && !allowNonSuggested) {
				throw new IllegalStateException("Mark not in suggestions.");
			}
		}, false);
	}

	/**
	 * Reject all suggestions for a submission. Creates a review with REJECTED decision.
	 * Enforces: processing must be COMPLETED; review must not already exist.
	 */
	@Transactional
	public MarkEvidenceReview rejectAll(Long submissionId, String comment) {
		return processReview(submissionId, ReviewDecision.REJECTED, null, comment, null, false);
	}

	/**
	 * Returns the current review decision for a submission, if present.
	 */
	@Transactional(readOnly = true)
	public ReviewDecision getReviewStatus(Long submissionId) {
		return reviewRepository.findBySubmissionId(submissionId)
				.map(MarkEvidenceReview::getDecision)
				.orElse(null);
	}

	/**
	 * The "Template" that handles the repetitive lifecycle of a review.
	 */
	private MarkEvidenceReview processReview(
			Long submissionId,
			ReviewDecision decision,
			Mark mark,
			String comment,
			Consumer<UUID> extraValidation,
			boolean isDiscovery) {

		MarkEvidenceSubmission submission = submissionQueryService.findById(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

		var overview = processingQueryService.findOverviewBySubmissionId(submissionId)
				.orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " not processed"));

		// Pass isDiscovery to allow bypassing "No suggestions available" check
		validateState(submissionId, overview.getStatus(), suggestionQueryService.countByProcessingId(overview.getId()), isDiscovery);

		if (extraValidation != null) extraValidation.accept(overview.getId());

		MarkEvidenceReview review = MarkEvidenceReview.builder()
				.submission(submission)
				.selectedMark(mark)
				.decision(decision)
				.reviewedAt(Instant.now())
				.comment(comment)
				.build();

		SecurityUtils.getCurrentUserId().flatMap(userQueryService::findById).ifPresent(review::setReviewedBy);

		try {
			MarkEvidenceReview saved = reviewRepository.save(review);
			recordMetrics(saved, overview.getId());

			Long reviewerId = saved.getReviewedBy() == null ? null : saved.getReviewedBy().getId();
			Long selectedMarkId = saved.getSelectedMark() == null ? null : saved.getSelectedMark().getId();
			eventPublisher.publish(new ReviewCompletedEvent(submissionId, decision, selectedMarkId, reviewerId));

			return saved;
		} catch (DataIntegrityViolationException dive) {
			throw new IllegalStateException("Submission " + submissionId + " already reviewed");
		}
	}

	private void validateState(Long submissionId, ProcessingStatus status, long count, boolean isDiscovery) {
		if (status != ProcessingStatus.COMPLETED && status != ProcessingStatus.REVIEW_PENDING) {
			throw new IllegalStateException("Processing not ready.");
		}
		// If this ISN'T a discovery/new mark, we might require suggestions to exist
		if (!isDiscovery && !allowEmptyReview && count == 0) {
			throw new IllegalStateException("No suggestions available to review.");
		}
		if (reviewRepository.existsBySubmissionId(submissionId)) {
			throw new IllegalStateException("Submission already reviewed.");
		}
	}

	private void recordMetrics(MarkEvidenceReview review, UUID processingId) {
		try {
			meterRegistry.counter("review.decisions.count", "decision", review.getDecision().name()).increment();

			if (review.getDecision() == ReviewDecision.APPROVED && review.getSelectedMark() != null) {
				suggestionQueryService.findByProcessingIdAndMarkId(processingId, review.getSelectedMark().getId())
						.ifPresent(s -> meterRegistry.summary("review.accepted.confidence").record(s.getConfidence()));
			}
		} catch (Exception e) {
			log.debug("Metric recording failed: {}", e.getMessage());
		}
	}
}
