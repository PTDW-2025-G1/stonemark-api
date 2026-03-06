package pt.estga.decision.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.content.entities.MarkOccurrence;
import pt.estga.content.services.MarkOccurrenceService;
import pt.estga.content.services.MonumentService;
import pt.estga.decision.services.DecisionServiceFactory;
import pt.estga.decision.services.ProposalDecisionService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.events.ProposalAcceptedEvent;
import pt.estga.proposal.events.ProposalScoredEvent;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProposalEventListener {

    private final DecisionServiceFactory decisionServiceFactory;
    private final MarkOccurrenceProposalRepository proposalRepo;
    private final MonumentService monumentService;
    private final MarkOccurrenceService markOccurrenceService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProposalScored(ProposalScoredEvent event) {
        var proposalId = event.getProposalId();
        log.info("Starting async automatic decision process for proposal ID: {}", proposalId);

        ProposalDecisionService<?> decisionService = decisionServiceFactory.getServiceForProposalId(proposalId);
        decisionService.makeAutomaticDecision(proposalId);
        log.info("Completed async automatic decision process for proposal ID: {}", proposalId);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProposalAccepted(ProposalAcceptedEvent event) {
        if (event.getProposal() == null) {
            log.warn("Received ProposalAcceptedEvent without proposal payload");
            return;
        }

        var proposalId = event.getProposal().getId();
        log.info("Starting async acceptance processing for proposal ID: {}", proposalId);

        proposalRepo.findById(proposalId).ifPresentOrElse(proposal -> {
            activateExistingMonumentIfNeeded(proposal);
            createOccurrenceFromAcceptedProposal(proposal);
            log.info("Completed async acceptance processing for proposal ID: {}", proposalId);
        }, () -> log.error("Proposal with ID {} not found during async acceptance processing", proposalId));
    }

    private void activateExistingMonumentIfNeeded(MarkOccurrenceProposal proposal) {
        if (proposal.getExistingMonument() != null && !proposal.getExistingMonument().getActive()) {
            var monument = proposal.getExistingMonument();
            monument.setActive(true);
            monumentService.update(monument);
            log.info("Activated monument ID: {} as part of proposal acceptance", monument.getId());
        }
    }

    private void createOccurrenceFromAcceptedProposal(MarkOccurrenceProposal proposal) {
        MarkOccurrence occurrence = MarkOccurrence.builder()
                .mark(proposal.getExistingMark())
                .monument(proposal.getExistingMonument())
                .author(proposal.getSubmittedBy())
                .embedding(proposal.getEmbedding())
                .build();

        Long coverId = proposal.getOriginalMediaFile() != null ? proposal.getOriginalMediaFile().getId() : null;

        try {
            MarkOccurrence saved = markOccurrenceService.create(occurrence, null, coverId);
            log.info("Created mark occurrence ID: {} from accepted proposal ID: {}", saved.getId(), proposal.getId());
        } catch (IOException e) {
            log.error("Failed to create mark occurrence from accepted proposal ID: {}", proposal.getId(), e);
        }
    }
}
