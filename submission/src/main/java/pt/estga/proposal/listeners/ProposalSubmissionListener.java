package pt.estga.proposal.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.events.ProposalScoredEvent;
import pt.estga.proposal.events.ProposalSubmittedEvent;
import pt.estga.proposal.repositories.ProposalRepository;
import pt.estga.proposal.services.ProposalScoringService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProposalSubmissionListener {

    private final ProposalRepository<Proposal> proposalRepository;
    private final ProposalScoringService scoringService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProposalSubmitted(ProposalSubmittedEvent event) {
        Long proposalId = event.getProposalId();
        log.info("Processing submission asynchronously for proposal ID: {}", proposalId);

        try {
            proposalRepository.findById(proposalId).ifPresentOrElse(proposal -> {
                try {
                    // Calculate scores
                    Integer priority = scoringService.calculatePriority(proposal);
                    Integer credibility = scoringService.calculateCredibilityScore(proposal);

                    proposal.setPriority(priority);
                    proposal.setCredibilityScore(credibility);
                    
                    proposalRepository.save(proposal);
                    log.info("Scores updated for proposal ID: {}. Priority={}, Credibility={}", proposalId, priority, credibility);

                    // Publish event indicating scoring is complete
                    eventPublisher.publishEvent(new ProposalScoredEvent(this, proposalId));

                } catch (Exception e) {
                    log.error("Error calculating scores for proposal ID: {}", proposalId, e);
                    // In a real system, we might want to send this to a dead-letter queue or retry
                }
            }, () -> log.warn("Proposal with ID {} not found during async submission processing", proposalId));
        } catch (Exception e) {
            log.error("Unexpected error in proposal submission listener for proposal ID: {}", proposalId, e);
        }
    }
}
