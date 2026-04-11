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

import java.time.Instant;
import java.util.List;

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
		if (processing.getStatus() != pt.estga.processing.enums.ProcessingStatus.COMPLETED) {
			throw new IllegalStateException("Cannot review submission " + submissionId + " before processing is COMPLETED");
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
			// we allow acceptance of non-suggested marks but log a warning. Alternatively, throw if you want stricter behavior.
		}

		MarkEvidenceReview review = MarkEvidenceReview.builder()
				.submission(submission)
				.selectedMark(mark)
				.decision(ReviewDecision.APPROVED)
				.reviewedAt(Instant.now())
				.build();

		MarkEvidenceReview saved = reviewRepository.save(review);

		// If accepted, mark submission as PROCESSED (considered resolved)
		submission.setStatus(pt.estga.intake.enums.SubmissionStatus.PROCESSED);
		submissionCommandService.update(submission);

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
		if (processing.getStatus() != pt.estga.processing.enums.ProcessingStatus.COMPLETED) {
			throw new IllegalStateException("Cannot review submission " + submissionId + " before processing is COMPLETED");
		}

		// Idempotency
		if (reviewRepository.existsBySubmissionId(submissionId)) {
			throw new IllegalStateException("Submission " + submissionId + " has already been reviewed");
		}

		MarkEvidenceReview review = MarkEvidenceReview.builder()
				.submission(submission)
				.selectedMark(null)
				.decision(ReviewDecision.REJECTED)
				.reviewedAt(Instant.now())
				.build();

		return reviewRepository.save(review);
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
