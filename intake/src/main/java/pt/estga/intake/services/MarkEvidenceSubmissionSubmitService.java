package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.fileapi.FileStorageOperations;
import pt.estga.shared.events.AfterCommitEventPublisher;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmissionSubmitService {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final FileStorageOperations fileStorage;
    private final AfterCommitEventPublisher eventPublisher;

    /**
     * Submits a mark evidence submission. The provided submission object already contains user and source information
     * (if available) and will be preserved. The method commits the staged file, attaches the
     * resulting MediaFile to the submission, sets the status to RECEIVED, persists the submission and
     * publishes a MarkEvidenceSubmittedEvent after commit.
     */
    @Transactional
    public void submit(
            MarkEvidenceSubmission submission,
            UUID stagedFileId,
            String photoFilename
    ) throws IOException {

        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }

        if (stagedFileId == null) {
            throw new IllegalArgumentException("Staged file ID is required");
        }

        String safeFilename = (photoFilename == null || photoFilename.isBlank()) ? "upload.jpg" : photoFilename;

        var mediaFile = fileStorage.commit(stagedFileId, safeFilename);
        submission.setOriginalMediaFileId(mediaFile.id());

        submission.setStatus(SubmissionStatus.RECEIVED);
        MarkEvidenceSubmission saved = submissionRepository.save(submission);

        eventPublisher.publish(new MarkEvidenceSubmittedEvent(this, saved.getId()));

        log.info("Submission submitted successfully with ID: {}", saved.getId());
    }
}
