package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.upload.MediaUploadOrchestrator;
import pt.estga.shared.events.AfterCommitEventPublisher;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmissionSubmitService {

    private final MarkEvidenceSubmissionCommandService commandService;
    private final MediaUploadOrchestrator mediaUploadOrchestrator;
    private final AfterCommitEventPublisher eventPublisher;

    /**
     * Submits a mark evidence submission. The provided submission object already contains user and source information
     * (if available) and will be preserved. The method stores the provided photo bytes, attaches the
     * resulting MediaFile to the submission, sets the status to SUBMITTED, persists the submission and
     * publishes a MarkEvidenceSubmittedEvent after commit.
     */
    @Transactional
    public void submit(
            MarkEvidenceSubmission submission,
            byte[] photoData,
            String photoFilename
    ) throws IOException {

        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }

        if (photoData == null || photoData.length == 0) {
            throw new IllegalArgumentException("Submission photo is required");
        }

        // Use a safe filename when none is provided
        String safeFilename = (photoFilename == null || photoFilename.isBlank()) ? "upload.jpg" : photoFilename;

        // Save the photo using the orchestrator (non-deprecated API)
        MediaFile mediaFile = mediaUploadOrchestrator.orchestrateUpload(new ByteArrayInputStream(photoData), safeFilename);
        submission.setOriginalMediaFile(mediaFile);

        // Preserve submittedBy and submissionSource that may already be present on the submission

        // Mark as submitted and persist
        submission.setStatus(SubmissionStatus.RECEIVED);
        MarkEvidenceSubmission saved = commandService.create(submission);

        // Publish event after transaction commit
        eventPublisher.publish(new MarkEvidenceSubmittedEvent(this, saved.getId()));

        log.info("Submission submitted successfully with ID: {}", saved.getId());
    }
}
