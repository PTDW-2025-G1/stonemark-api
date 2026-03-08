package pt.estga.proposal.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.detection.model.DetectionResult;
import pt.estga.detection.service.DetectionService;
import pt.estga.file.services.MediaService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.events.ProposalPhotoUploadedEvent;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProposalEnrichmentListener {

    private final MarkOccurrenceProposalRepository proposalRepo;
    private final DetectionService detectionService;
    private final MediaService mediaService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProposalPhotoUploaded(ProposalPhotoUploadedEvent event) {
        MarkOccurrenceProposal proposal = event.getProposal();
        log.info("Async detection processing for proposal");

        try (InputStream detectionInputStream = mediaService.loadFileById(proposal.getOriginalMediaFile().getId()).getInputStream()) {
            DetectionResult detectionResult = detectionService.detect(detectionInputStream, proposal.getOriginalMediaFile().getOriginalFilename());
            if (detectionResult != null && detectionResult.embedding() != null && detectionResult.embedding().length > 0) {
                proposal.setEmbedding(detectionResult.embedding());
                
                // If the proposal has already been persisted (e.g. user finished flow quickly), update it in DB
                if (proposal.getId() != null) {
                    log.info("Proposal already submitted (ID: {}), updating embedding in DB", proposal.getId());
                    proposalRepo.save(proposal);
                } else {
                    log.info("Proposal not yet submitted, embedding set in memory object");
                }
            } else {
                log.info("No embedding detected for proposal");
            }
        } catch (Exception e) {
            log.warn("Detection service failed for proposal. Proceeding without detection.", e);
        }
    }
}
