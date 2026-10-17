package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.core.events.AfterCommitEventPublisher;
import pt.estga.file.api.FileStorageOperations;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;

import java.util.UUID;

/**
 * Persists a submission and its media in a single transaction.
 *
 * <p>{@link FileStorageOperations#commit} joins this transaction, so a failure
 * rolls back both the media metadata and the submission row, and object storage
 * is compensated on rollback. The submitted event is published only after commit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmissionSubmitService {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final FileStorageOperations fileStorage;
    private final AfterCommitEventPublisher eventPublisher;

    @Transactional
    public UUID submit(MarkEvidenceSubmission submission, UUID stagedFileId, String photoFilename) {
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
        eventPublisher.publish(new MarkEvidenceSubmittedEvent(saved.getId()));

        log.info("Submission submitted successfully with ID: {}", saved.getId());
        return saved.getId();
    }
}
