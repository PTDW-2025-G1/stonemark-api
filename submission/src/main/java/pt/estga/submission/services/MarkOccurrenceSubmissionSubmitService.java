package pt.estga.submission.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.api.MediaService;
import pt.estga.submission.entities.MarkEvidenceSubmission;
import pt.estga.submission.enums.SubmissionSource;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.events.SubmissionSubmittedEvent;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class MarkOccurrenceSubmissionSubmitService {

    private final MarkOccurrenceSubmissionRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaService mediaService;

    /**
     * Submits a submission with optional image file.
     * Saves the image if provided and then submits the submission.
     */
    @Transactional
    public MarkEvidenceSubmission submit(MarkEvidenceSubmission submission, MultipartFile imageFile) throws IOException {
        log.info("Submitting submission of type: {}", submission.getClass().getSimpleName());

        if (SubmissionStatus.SUBMITTED.equals(submission.getStatus())) {
            log.warn("Submission is already submitted. Skipping submission logic.");
            return submission;
        }

        // Save the image file if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            log.debug("Saving image file for submission: {}", imageFile.getOriginalFilename());
            MediaFile mediaFile = mediaService.save(imageFile.getInputStream(), imageFile.getOriginalFilename());
            submission.setOriginalMediaFile(mediaFile);
        }

        submission.setStatus(SubmissionStatus.SUBMITTED);

        MarkEvidenceSubmission savedSubmission = repository.save(submission);
        log.info("Submission submitted successfully with ID: {}", savedSubmission.getId());

        eventPublisher.publishEvent(new SubmissionSubmittedEvent(this, savedSubmission.getId()));
        log.debug("Published SubmissionSubmittedEvent for submission ID: {}", savedSubmission.getId());

        return savedSubmission;
    }

    /**
     * Overloaded submit method for backward compatibility - submits without image file
     */
    @Transactional
    public void submit(MarkEvidenceSubmission submission) throws IOException {
        submit(submission, null);
    }

    /**
     * Submits a submission collected from the chatbot flow.
     * This method saves the photo, creates the submission, and submits it in one transaction.
     */
    @Transactional
    public void submitFromChatbot(
            MarkEvidenceSubmission proposal,
            byte[] photoData,
            String photoFilename,
            User user,
            SubmissionSource source) throws IOException {

        if (photoData == null || photoData.length == 0) {
            throw new IllegalArgumentException("Submission photo is required");
        }

        String safeFilename = (photoFilename == null || photoFilename.isBlank())
                ? "chatbot-upload.jpg"
                : photoFilename;

        Long userId = user != null ? user.getId() : null;
        log.info("Submitting chatbot submission for user ID: {} with source: {}", userId, source);

        // Save the photo
        MediaFile mediaFile = mediaService.save(new ByteArrayInputStream(photoData), safeFilename);
        proposal.setOriginalMediaFile(mediaFile);

        // Set optional user and source (anonymous chatbot submissions are allowed)
        proposal.setSubmittedBy(user);
        proposal.setSubmissionSource(source != null ? source : SubmissionSource.OTHER);

        // Submit the submission (this persists it and emits SubmissionSubmittedEvent)
        submit(proposal);
    }
}
