package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.fileapi.FileStorageOperations;
import pt.estga.sharedcore.events.AfterCommitEventPublisher;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionStatus;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.territory.services.DivisionService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkEvidenceSubmissionSubmitService {

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final FileStorageOperations fileStorage;
    private final AfterCommitEventPublisher eventPublisher;
    private final DivisionService divisionService;

    @Transactional
    public void submit(
            MarkEvidenceSubmission submission,
            UUID stagedFileId,
            String photoFilename
    ) {

        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }

        if (stagedFileId == null) {
            throw new IllegalArgumentException("Staged file ID is required");
        }

        String safeFilename = (photoFilename == null || photoFilename.isBlank()) ? "upload.jpg" : photoFilename;

        var mediaFile = fileStorage.commit(stagedFileId, safeFilename);
        submission.setOriginalMediaFileId(mediaFile.id());

        tagDivision(submission);

        submission.setStatus(SubmissionStatus.RECEIVED);
        MarkEvidenceSubmission saved = submissionRepository.save(submission);

        eventPublisher.publish(new MarkEvidenceSubmittedEvent(this, saved.getId()));

        log.info("Submission submitted successfully with ID: {}", saved.getId());
    }

    private void tagDivision(MarkEvidenceSubmission submission) {
        if (submission.getLatitude() == null || submission.getLongitude() == null) return;
        divisionService.findLowestContainingDivision(submission.getLatitude(), submission.getLongitude())
                .ifPresent(submission::setDivision);
    }
}
