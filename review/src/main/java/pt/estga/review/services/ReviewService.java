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
	 * Accept a suggested mark for the given submission.
	 * Enforces: processing must be COMPLETED; review must not already exist; if accepted, mark submission as PROCESSED.
	 */
	@Transactional
	public MarkEvidenceReview acceptSuggestion(Long submissionId, Long markId) {
		MarkEvidenceSubmission submission = submissionQueryService.findById(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission with id " + submissionId + " not found"));

		// Ensure processing exists and is completed
		MarkEvidenceProcessing processing = processingRepository.findBySubmissionId(submissionId)
				.orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " has not been processed"));
		if (!processing.isReadyForReview()) {
			throw new IllegalStateException("Cannot review submission " + submissionId + " before processing is ready for review");
		}

		// Prevent reviewing when there are no suggestions unless explicitly allowed
		if (!allowEmptyReview && (processing.getSuggestions() == null || processing.getSuggestions().isEmpty())) {
			throw new IllegalStateException("Cannot review submission " + submissionId + " because there are no suggestions available");
		}

		// Idempotency: don't allow a second review
		if (reviewRepository.existsBySubmissionId(submissionId)) {
			throw new IllegalStateException("Submission " + submissionId + " has already been reviewed");
		}

		// Ensure mark exists and was among suggestions for this processing
		Mark mark = markRepository.findById(markId)
				.orElseThrow(() -> new ResourceNotFoundException("Mark with id " + markId + " not found"));

		boolean suggested = suggestionRepository.existsByProcessingIdAndMarkId(processing.getId(), markId);
		if (!suggested) {
			log.warn("Accepted mark {} was not present in suggestions for submission {}", markId, submissionId);
			if (!allowNonSuggested) {
				throw new IllegalStateException("Accepted mark was not present in suggestions and non-suggested acceptance is disabled");
			}
		}

		MarkEvidenceReview review = MarkEvidenceReview.builder()
				.submission(submission)
				.selectedMark(mark)
				.reviewedAt(Instant.now())
				.build();

		review.setDecision(ReviewDecision.APPROVED);
		// set reviewer if available
		SecurityUtils.getCurrentUserId().flatMap(userRepository::findById).ifPresent(review::setReviewedBy);

		MarkEvidenceReview saved;
		try {
			saved = reviewRepository.save(review);
		} catch (DataIntegrityViolationException dive) {
			log.warn("Concurrent review attempted for submission {}: {}", submissionId, dive.getMessage());
			throw new IllegalStateException("Submission " + submissionId + " has already been reviewed");
		}

		// Metrics: decision counts
		try {
			meterRegistry.counter("review.decisions.count", "decision", saved.getDecision().name()).increment();
		} catch (Exception e) {
			log.debug("Failed to increment review decision metric for submission {}: {}", submissionId, e.getMessage());
		}

		// If approved, record average confidence for accepted suggestion (if available)
		if (saved.getDecision() == ReviewDecision.APPROVED && saved.getSelectedMark() != null) {
			try {
				java.util.UUID processingId = processing.getId();
				suggestionRepository.findByProcessingIdAndMarkId(processingId, saved.getSelectedMark().getId())
					.ifPresent(s -> meterRegistry.summary("review.accepted.suggestion.confidence", "submission", submissionId.toString())
						.record(s.getConfidence()));
			} catch (Exception e) {
				log.debug("Failed to record accepted suggestion confidence for submission {}: {}", submissionId, e.getMessage());
			}
		}

		// Publish domain event after commit to move state transitions out of the write path.
		Long reviewerId = saved.getReviewedBy() == null ? null : saved.getReviewedBy().getId();
		Long selectedMarkId = saved.getSelectedMark() == null ? null : saved.getSelectedMark().getId();
		eventPublisher.publish(new ReviewCompletedEvent(submissionId, ReviewDecision.APPROVED, selectedMarkId, reviewerId));

		return saved;
	}

	/**
	 * Reject all suggestions for a submission. Creates a review with REJECTED decision.
	 * Enforces: processing must be COMPLETED; review must not already exist.
	 */
	@Transactional
	public MarkEvidenceReview rejectAll(Long submissionId) {
		MarkEvidenceSubmission submission = submissionQueryService.findById(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission with id " + submissionId + " not found"));

		// Ensure processing exists and is completed
		MarkEvidenceProcessing processing = processingRepository.findBySubmissionId(submissionId)
				.orElseThrow(() -> new IllegalStateException("Submission " + submissionId + " has not been processed"));
		if (!processing.isReadyForReview()) {
			throw new IllegalStateException("Cannot review submission " + submissionId + " before processing is ready for review");
		}

		// Prevent reviewing when there are no suggestions unless explicitly allowed
		if (!allowEmptyReview && (processing.getSuggestions() == null || processing.getSuggestions().isEmpty())) {
			throw new IllegalStateException("Cannot review submission " + submissionId + " because there are no suggestions available");
		}

		// Idempotency
		if (reviewRepository.existsBySubmissionId(submissionId)) {
			throw new IllegalStateException("Submission " + submissionId + " has already been reviewed");
		}

		MarkEvidenceReview review = MarkEvidenceReview.builder()
				.submission(submission)
				.selectedMark(null)
				.reviewedAt(Instant.now())
				.build();

		review.setDecision(ReviewDecision.REJECTED);
		SecurityUtils.getCurrentUserId().flatMap(userRepository::findById).ifPresent(review::setReviewedBy);

		MarkEvidenceReview saved;
		try {
			saved = reviewRepository.save(review);
		} catch (DataIntegrityViolationException dive) {
			log.warn("Concurrent review attempted for submission {}: {}", submissionId, dive.getMessage());
			throw new IllegalStateException("Submission " + submissionId + " has already been reviewed");
		}

		Long reviewerId = saved.getReviewedBy() == null ? null : saved.getReviewedBy().getId();
		eventPublisher.publish(new ReviewCompletedEvent(submissionId, ReviewDecision.REJECTED, null, reviewerId));

		return saved;
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
}
