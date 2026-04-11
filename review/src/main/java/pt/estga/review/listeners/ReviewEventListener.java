package pt.estga.review.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.services.MarkEvidenceSubmissionCommandService;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.events.ReviewCompletedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final MarkEvidenceSubmissionCommandService submissionCommandService;
    private final MarkEvidenceProcessingRepository processingRepository;

    @EventListener
    @Transactional
    public void handleReviewCompleted(ReviewCompletedEvent event) {
        Long submissionId = event.getSubmissionId();
        try {
            // Set submission status according to review decision. Use explicit domain statuses
            submissionQueryService.findById(submissionId).ifPresent(s -> {
                if (event.getDecision() == ReviewDecision.APPROVED) {
                    s.setStatus(SubmissionStatus.PROCESSED);
                } else if (event.getDecision() == ReviewDecision.REJECTED) {
                    s.setStatus(SubmissionStatus.REJECTED);
                } else {
                    // For NO_MATCH / FLAGGED keep as PROCESSED to indicate manual review completed
                    s.setStatus(SubmissionStatus.PROCESSED);
                }
                submissionCommandService.update(s);
            });

            // Transition processing to REVIEWED if present
            processingRepository.findBySubmissionId(submissionId).ifPresent(p -> {
                p.setStatus(ProcessingStatus.REVIEWED);
                processingRepository.save(p);
            });
        } catch (Exception e) {
            // Listeners should not throw - log and continue
            log.error("Failed to apply post-review state transitions for submission {}: {}", submissionId, e.getMessage(), e);
        }
    }
}
