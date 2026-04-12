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
import pt.estga.review.enums.ReviewType;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.review.services.markevidencereview.MarkEvidenceReviewCommandService;
import pt.estga.review.services.markevidencereview.MarkEvidenceReviewQueryService;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import pt.estga.shared.utils.SecurityUtils;
import pt.estga.user.services.UserQueryService;
import pt.estga.shared.events.AfterCommitEventPublisher;
import pt.estga.review.events.ReviewCompletedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
 	private final MarkEvidenceReviewCommandService markEvidenceReviewCommandService;
 	private final MarkEvidenceReviewQueryService markEvidenceReviewQueryService;
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

		// 2. Defer mark creation so it happens inside the same transaction as the review.
		Supplier<Mark> markSupplier = () -> markCommandService.create(Mark.builder().title(newMarTitle).build());

		// 3. Process; pass ReviewType.DISCOVERY to indicate this is a new-mark flow.
		return processReview(submissionId, ReviewDecision.APPROVED, null, markSupplier, comment, null, ReviewType.DISCOVERY);
	}

	/**
	 * Accept a suggested mark for the given submission.
	 * Enforces: processing must be COMPLETED; review must not already exist; if accepted, mark submission as PROCESSED.
	 */
	@Transactional
	public MarkEvidenceReview acceptSuggestion(Long submissionId, Long markId, String comment) {
		Mark mark = markQueryService.findById(markId)
				.orElseThrow(() -> new ResourceNotFoundException("Mark " + markId + " not found"));

		return processReview(submissionId, ReviewDecision.APPROVED, mark, null, comment, (procId) -> {
			if (!suggestionQueryService.existsByProcessingIdAndMarkId(procId, markId) && !allowNonSuggested) {
				throw new IllegalStateException("Mark not in suggestions.");
			}
		}, ReviewType.MATCH);
	}

	/**
	 * Reject all suggestions for a submission. Creates a review with REJECTED decision.
	 * Enforces: processing must be COMPLETED; review must not already exist.
	 */
	@Transactional
	public MarkEvidenceReview rejectAll(Long submissionId, String comment) {
		return processReview(submissionId, ReviewDecision.REJECTED, null, null, comment, null, ReviewType.REJECTION);
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

	/**
	 * The "Template" that handles the repetitive lifecycle of a review.
	 */
	private MarkEvidenceReview processReview(
			Long submissionId,
			ReviewDecision decision,
			Mark mark,
			Supplier<Mark> markSupplier,
			String comment,
			Consumer<UUID> extraValidation,
			ReviewType reviewType) {

		MarkEvidenceSubmission submission = submissionQueryService.findById(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

		var overview = processingQueryService.findOverviewBySubmissionId(submissionId)
				.orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " not processed"));

		// If a mark needs to be created (new/discovery flow), create it now so it participates in this transaction.
		if (mark == null && markSupplier != null) {
			mark = markSupplier.get();
		}

		// Pass reviewType to allow bypassing "No suggestions available" check for discovery flows
		validateState(submissionId, overview.getStatus(), suggestionQueryService.countByProcessingId(overview.getId()), reviewType);

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
			MarkEvidenceReview saved = markEvidenceReviewCommandService.create(review);
			recordMetrics(saved, overview.getId());

			// Determine resulting submission status explicitly and publish it with the event so listeners
			SubmissionStatus resultingStatus;
			if (decision == ReviewDecision.APPROVED) {
				resultingStatus = SubmissionStatus.PROCESSED;
			} else if (decision == ReviewDecision.REJECTED) {
				resultingStatus = SubmissionStatus.REJECTED;
			} else {
				resultingStatus = SubmissionStatus.PROCESSED;
			}

			eventPublisher.publish(ReviewCompletedEvent.builder()
					.submissionId(submissionId)
					.decision(decision)
					.reviewId(saved.getId())
					.resultingSubmissionStatus(resultingStatus)
					.build());

			return saved;
		} catch (DataIntegrityViolationException dive) {
			throw new IllegalStateException("Submission " + submissionId + " already reviewed");
		}
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
