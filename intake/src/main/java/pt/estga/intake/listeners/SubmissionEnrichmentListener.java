package pt.estga.intake.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.vision.DetectionResult;
import pt.estga.vision.DetectionService;
import pt.estga.file.application.MediaService;
import pt.estga.intake.events.MarkEvidenceSubmittedEvent;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEnrichmentListener {

    private final MarkEvidenceSubmissionRepository proposalRepo;
    private final DetectionService detectionService;
    private final MediaService mediaService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProposalSubmitted(MarkEvidenceSubmittedEvent event) {
        Long proposalId = event.getSubmissionId();
        log.info("Async detection processing for submitted proposal ID: {}", proposalId);

        proposalRepo.findById(proposalId).ifPresentOrElse(proposal -> {
            if (proposal.getOriginalMediaFile() == null) {
                log.info("Proposal ID {} has no original media file. Skipping enrichment.", proposalId);
                return;
            }

            try (InputStream detectionInputStream = mediaService.loadFileById(proposal.getOriginalMediaFile().getId()).getInputStream()) {
                DetectionResult detectionResult = detectionService.detect(detectionInputStream, proposal.getOriginalMediaFile().getOriginalFilename());
                if (detectionResult != null && detectionResult.embedding() != null && detectionResult.embedding().length > 0) {
                    proposal.setEmbedding(detectionResult.embedding());
                    proposalRepo.save(proposal);
                    log.info("Embedding updated for proposal ID: {}", proposalId);
                } else {
                    log.info("No embedding detected for proposal ID: {}", proposalId);
                }
            } catch (Exception e) {
                log.warn("Detection service failed for proposal ID {}. Proceeding without detection.", proposalId, e);
            }
        }, () -> log.warn("Submitted proposal with ID {} not found during enrichment", proposalId));
    }
}
