package pt.estga.submission.services.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaService;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionSource;
import pt.estga.submission.services.submission.MarkOccurrenceProposalSubmissionService;
import pt.estga.user.entities.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalChatbotSubmitService {

    private final MediaService mediaService;
    private final MarkOccurrenceProposalSubmissionService submissionService;

    /**
     * Submits a proposal collected from the chatbot flow.
     * This method saves the photo, creates the proposal, and submits it in one transaction.
     */
    @Transactional
    public void submitFromChatbot(
            MarkOccurrenceSubmission proposal,
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
        log.info("Submitting chatbot proposal for user ID: {} with source: {}", userId, source);

        // Save the photo
        MediaFile mediaFile = mediaService.save(new ByteArrayInputStream(photoData), safeFilename);
        proposal.setOriginalMediaFile(mediaFile);

        // Set optional user and source (anonymous chatbot submissions are allowed)
        proposal.setSubmittedBy(user);
        proposal.setSubmissionSource(source != null ? source : SubmissionSource.OTHER);

        // Submit the proposal (this persists it and emits SubmissionSubmittedEvent)
        submissionService.submit(proposal);
    }
}
