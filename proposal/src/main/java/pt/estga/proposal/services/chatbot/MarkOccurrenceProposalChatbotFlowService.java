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
import pt.estga.user.entities.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MarkOccurrenceProposalChatbotFlowService {

    private final MediaService mediaService;
    private final ApplicationEventPublisher eventPublisher;

    public MarkOccurrenceProposal startProposal(User user, SubmissionSource source) {
        log.info("Starting new chatbot proposal for user ID: {} with source: {}", user.getId(), source);
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        proposal.setSubmissionSource(source);
        proposal.setSubmittedBy(user);
        return proposal;
    }

    @Transactional
    public void addPhoto(MarkOccurrenceProposal proposal, byte[] photoData, String filename) throws IOException {
        log.info("Adding photo for proposal");

        MediaFile mediaFile = mediaService.save(new ByteArrayInputStream(photoData), filename);
        proposal.setOriginalMediaFile(mediaFile);

        // Publish event for async processing (e.g., detection)
        eventPublisher.publishEvent(new ProposalPhotoUploadedEvent(this, proposal));
    }
}
