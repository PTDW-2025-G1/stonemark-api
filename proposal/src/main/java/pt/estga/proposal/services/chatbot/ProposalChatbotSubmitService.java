package pt.estga.proposal.services.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.enums.SubmissionSource;
import pt.estga.proposal.events.ProposalPhotoUploadedEvent;
import pt.estga.proposal.services.submission.MarkOccurrenceProposalSubmissionService;
import pt.estga.user.entities.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalChatbotSubmitService {

    private final MediaService mediaService;
    private final MarkOccurrenceProposalSubmissionService submissionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Submits a proposal collected from the chatbot flow.
     * This method saves the photo, creates the proposal, and submits it in one transaction.
     */
    @Transactional
    public void submitFromChatbot(
            MarkOccurrenceProposal proposal,
            byte[] photoData,
            String photoFilename,
            User user,
            SubmissionSource source) throws IOException {

        log.info("Submitting chatbot proposal for user ID: {} with source: {}", user.getId(), source);

        // Save the photo
        MediaFile mediaFile = mediaService.save(new ByteArrayInputStream(photoData), photoFilename);
        proposal.setOriginalMediaFile(mediaFile);

        // Set user and source
        proposal.setSubmittedBy(user);
        proposal.setSubmissionSource(source);

        // Publish event for async processing (e.g., detection) before submission
        eventPublisher.publishEvent(new ProposalPhotoUploadedEvent(this, proposal));

        // Submit the proposal
        submissionService.submit(proposal);
    }
}

