package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.MarkEvidenceSubmissionCommandService;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import pt.estga.shared.utils.SecurityUtils;
import pt.estga.user.repositories.UserRepository;
import pt.estga.processing.enums.ProcessingStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

	private final MarkEvidenceSubmissionQueryService submissionQueryService;
	private final MarkEvidenceSubmissionCommandService submissionCommandService;
	private final MarkEvidenceProcessingRepository processingRepository;
	private final MarkSuggestionRepository suggestionRepository;
	private final MarkRepository markRepository;
	private final MarkEvidenceReviewRepository reviewRepository;
	private final UserRepository userRepository;

	@Value("${review.allow-non-suggested:false}")
	private boolean allowNonSuggested;

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

		// Idempotency: don't allow a second review
		if (reviewRepository.existsBySubmissionId(submissionId)) {
			throw new IllegalStateException("Submission " + submissionId + " has already been reviewed");
		}

		// Ensure mark exists and was among suggestions for this processing
		Mark mark = markRepository.findById(markId)
				.orElseThrow(() -> new ResourceNotFoundException("Mark with id " + markId + " not found"));

		List<MarkSuggestion> suggestions = suggestionRepository.findByProcessingId(processing.getId());
		boolean suggested = suggestions.stream().anyMatch(s -> s.getMark() != null && markId.equals(s.getMark().getId()));
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

		MarkEvidenceReview saved = reviewRepository.save(review);

		// If accepted, mark submission as PROCESSED (considered resolved)
		submission.setStatus(pt.estga.intake.enums.SubmissionStatus.PROCESSED);
		submissionCommandService.update(submission);

		// Transition processing to REVIEWED
		processing.setStatus(ProcessingStatus.REVIEWED);
		processingRepository.save(processing);

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

		MarkEvidenceReview saved = reviewRepository.save(review);

		// Mark submission as PROCESSED as well to keep lifecycle consistent
		submission.setStatus(pt.estga.intake.enums.SubmissionStatus.PROCESSED);
		submissionCommandService.update(submission);

		// Transition processing to REVIEWED
		processing.setStatus(ProcessingStatus.REVIEWED);
		processingRepository.save(processing);

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
