package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import pt.estga.shared.utils.SecurityUtils;
import pt.estga.user.repositories.UserRepository;
import pt.estga.shared.events.AfterCommitEventPublisher;
import pt.estga.review.events.ReviewCompletedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

	private final MarkEvidenceSubmissionQueryService submissionQueryService;
	private final MarkEvidenceProcessingRepository processingRepository;
	private final MarkSuggestionRepository suggestionRepository;
	private final MarkRepository markRepository;
	private final MarkEvidenceReviewRepository reviewRepository;
	private final UserRepository userRepository;
	private final AfterCommitEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

	@Value("${review.allow-non-suggested:false}")
	private boolean allowNonSuggested;

	@Value("${review.allow-empty-review:false}")
	private boolean allowEmptyReview;

	/**
	 * Accept the submission with a new mark (not from suggestions). Creates a review with APPROVED decision.
	 * Enforces: processing must be COMPLETED; review must not already exist; if accepted, mark submission as PROCESSED.
	 */
	@Transactional
	public MarkEvidenceReview acceptAsNew(Long submissionId, String newMarTitle, String comment) {
		// 1. We have to check the suggestions FIRST because once we enter
		// processReview, we want to be sure we are allowed to create this mark.
		MarkEvidenceProcessing processing = processingRepository.findBySubmissionId(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Processing not found"));

		if (processing.getSuggestions() != null && !processing.getSuggestions().isEmpty()) {
			throw new IllegalStateException("Cannot create a new mark when suggestions exist.");
		}

		// 2. Now that we know it's safe, we create the mark
		Mark newMark = markRepository.save(Mark.builder().title(newMarTitle).build());

		// 3. And pass it into the template
		return processReview(submissionId, ReviewDecision.APPROVED, newMark, comment, null);
	}

	/**
	 * Accept a suggested mark for the given submission.
	 * Enforces: processing must be COMPLETED; review must not already exist; if accepted, mark submission as PROCESSED.
	 */
	@Transactional
	public MarkEvidenceReview acceptSuggestion(Long submissionId, Long markId, String comment) {
		Mark mark = markRepository.findById(markId)
				.orElseThrow(() -> new ResourceNotFoundException("Mark " + markId + " not found"));

		return processReview(submissionId, ReviewDecision.APPROVED, mark, comment, (proc) -> {
			boolean suggested = suggestionRepository.existsByProcessingIdAndMarkId(proc.getId(), markId);
			if (!suggested && !allowNonSuggested) {
				throw new IllegalStateException("Mark not in suggestions and non-suggested acceptance is disabled");
			}
		});
	}

	/**
	 * Reject all suggestions for a submission. Creates a review with REJECTED decision.
	 * Enforces: processing must be COMPLETED; review must not already exist.
	 */
	@Transactional
	public MarkEvidenceReview rejectAll(Long submissionId, String comment) {
		return processReview(submissionId, ReviewDecision.REJECTED, null, comment, null);
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
			Consumer<MarkEvidenceProcessing> extraValidation) {

		// 1. Fetch and Validate
		MarkEvidenceSubmission submission = submissionQueryService.findById(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));

		MarkEvidenceProcessing processing = processingRepository.findBySubmissionId(submissionId)
				.orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " not processed"));

		validateState(submissionId, processing);

		// Note: if extraValidation creates a Mark (acceptAsNew), we want to capture that.
		// For simplicity here, we'll keep your 'mark' parameter.
		if (extraValidation != null) extraValidation.accept(processing);

		// 2. Build Entity
		MarkEvidenceReview review = MarkEvidenceReview.builder()
				.submission(submission)
				.selectedMark(mark)
				.decision(decision)
				.reviewedAt(Instant.now())
				.comment(comment)
				.build();

		SecurityUtils.getCurrentUserId().flatMap(userRepository::findById).ifPresent(review::setReviewedBy);

		// 3. Persist with Idempotency Check
		MarkEvidenceReview saved;
		try {
			saved = reviewRepository.save(review);
		} catch (DataIntegrityViolationException dive) {
			throw new IllegalStateException("Submission " + submissionId + " already reviewed");
		}

		// 4. Side Effects (Metrics & Events)
		recordMetrics(saved, processing);

		Long reviewerId = saved.getReviewedBy() == null ? null : saved.getReviewedBy().getId();
		Long selectedMarkId = saved.getSelectedMark() == null ? null : saved.getSelectedMark().getId();
		eventPublisher.publish(new ReviewCompletedEvent(submissionId, decision, selectedMarkId, reviewerId));

		return saved;
	}

	private void validateState(Long submissionId, MarkEvidenceProcessing processing) {
		if (!processing.isReviewable()) {
			throw new IllegalStateException("Processing not reviewable for submission " + submissionId);
		}
		if (!allowEmptyReview && (processing.getSuggestions() == null || processing.getSuggestions().isEmpty())) {
			throw new IllegalStateException("No suggestions available for submission " + submissionId);
		}
		if (reviewRepository.existsBySubmissionId(submissionId)) {
			throw new IllegalStateException("Submission " + submissionId + " already reviewed");
		}
	}

	private void recordMetrics(MarkEvidenceReview review, MarkEvidenceProcessing processing) {
		try {
			meterRegistry.counter("review.decisions.count", "decision", review.getDecision().name()).increment();

			if (review.getDecision() == ReviewDecision.APPROVED && review.getSelectedMark() != null) {
				suggestionRepository.findByProcessingIdAndMarkId(processing.getId(), review.getSelectedMark().getId())
						.ifPresent(s -> meterRegistry.summary("review.accepted.confidence").record(s.getConfidence()));
			}
		} catch (Exception e) {
			log.debug("Metric recording failed: {}", e.getMessage());
		}
	}
}
