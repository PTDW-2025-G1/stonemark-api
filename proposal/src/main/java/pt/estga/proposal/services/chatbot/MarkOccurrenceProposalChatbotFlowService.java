package pt.estga.proposal.services.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.Monument;
import pt.estga.content.services.MarkQueryService;
import pt.estga.content.services.MarkSearchService;
import pt.estga.content.services.MonumentQueryService;
import pt.estga.file.entities.MediaFile;
import pt.estga.file.services.MediaService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.enums.SubmissionSource;
import pt.estga.proposal.events.ProposalPhotoUploadedEvent;
import pt.estga.user.entities.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MarkOccurrenceProposalChatbotFlowService {

    private final MediaService mediaService;
    private final MonumentQueryService monumentQueryService;
    private final MarkQueryService markQueryService;
    private final MarkSearchService markSearchService;
    private final ApplicationEventPublisher eventPublisher;

    private static final double COORDINATE_SEARCH_RANGE = 0.01;

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

    public List<Monument> suggestMonuments(MarkOccurrenceProposal proposal) {
        if (proposal.getLatitude() != null && proposal.getLongitude() != null) {
            log.info("Searching for monuments near lat: {}, lon: {} with range: {}", 
                    proposal.getLatitude(), proposal.getLongitude(), COORDINATE_SEARCH_RANGE);
            
            List<Monument> monuments = monumentQueryService.findByCoordinatesInRange(
                    proposal.getLatitude(), proposal.getLongitude(), COORDINATE_SEARCH_RANGE
            );
            log.info("Found {} monuments nearby.", monuments.size());
            return monuments;
        }
        log.warn("Proposal has no coordinates, cannot suggest monuments.");
        return List.of();
    }

    public List<Mark> suggestMarks(MarkOccurrenceProposal proposal) {
        if (proposal.getEmbedding() != null && proposal.getEmbedding().length > 0) {
            try {
                List<String> markIds = markSearchService.searchMarks(proposal.getEmbedding());
                return markIds.stream()
                        .map(Long::valueOf)
                        .map(markQueryService::findById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Mark search service failed for proposal. Proceeding without suggestions.", e);
            }
        }
        return List.of();
    }
}
