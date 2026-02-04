package pt.estga.decision.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.services.MonumentService;
import pt.estga.decision.services.DecisionServiceFactory;
import pt.estga.decision.services.ProposalDecisionService;
import pt.estga.proposal.events.ProposalAcceptedEvent;
import pt.estga.proposal.events.ProposalScoredEvent;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProposalEventListener {

    private final DecisionServiceFactory decisionServiceFactory;
    private final MarkOccurrenceProposalRepository proposalRepo;
    private final MonumentService monumentService;

    @Async
    @EventListener
    @Transactional
    public void handleProposalScored(ProposalScoredEvent event) {
        var proposalId = event.getProposalId();
        log.info("Starting async automatic decision process for proposal ID: {}", proposalId);

        ProposalDecisionService<?> decisionService = decisionServiceFactory.getServiceForProposalId(proposalId);
        decisionService.makeAutomaticDecision(proposalId);
        log.info("Completed async automatic decision process for proposal ID: {}", proposalId);
    }

    @Async
    @EventListener
    @Transactional
    public void handleProposalAccepted(ProposalAcceptedEvent event) {
        var proposalId = event.getProposal().getId();
        log.info("Starting async acceptance processing for proposal ID: {}", proposalId);

        if (event.getProposal() != null) {
            proposalRepo.findById(proposalId).ifPresentOrElse(p -> {
                if (p.getExistingMonument() != null && !p.getExistingMonument().getActive()) {
                    var monument = p.getExistingMonument();
                    monument.setActive(true);
                    monumentService.update(monument);
                    log.info("Activated monument ID: {} as part of proposal acceptance", monument.getId());
                }
                log.info("Completed async acceptance processing for proposal ID: {}", proposalId);
            }, () -> log.error("Proposal with ID {} not found during async acceptance processing", proposalId));
        }
    }
}
