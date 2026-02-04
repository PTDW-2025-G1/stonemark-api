package pt.estga.decision.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.decision.entities.ProposalDecisionAttempt;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.enums.DecisionType;
import pt.estga.decision.repositories.ProposalDecisionAttemptRepository;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.proposal.repositories.ProposalRepository;
import pt.estga.shared.exceptions.ResourceNotFoundException;
import pt.estga.user.entities.User;

import java.time.Instant;

@Slf4j
public abstract class ProposalDecisionService<T extends Proposal> {

    protected final ProposalDecisionAttemptRepository attemptRepo;
    protected final ProposalRepository<T> proposalRepo;
    protected final ApplicationEventPublisher eventPublisher;
    @Getter
    protected final Class<T> proposalType;

    protected ProposalDecisionService(
            ProposalDecisionAttemptRepository attemptRepo,
            ProposalRepository<T> proposalRepo,
            ApplicationEventPublisher eventPublisher,
            Class<T> proposalType
    ) {
        this.attemptRepo = attemptRepo;
        this.proposalRepo = proposalRepo;
        this.eventPublisher = eventPublisher;
        this.proposalType = proposalType;
    }

    /**
     * Triggers the automatic decision logic for a proposal by ID.
     */
    @Transactional
    public ProposalDecisionAttempt makeAutomaticDecision(Long proposalId) {
        log.info("Processing automatic decision for proposal ID: {}", proposalId);
        T proposal = getProposalOrThrow(proposalId);
        return makeAutomaticDecision(proposal);
    }

    /**
     * Triggers the automatic decision logic for a proposal entity.
     * Subclasses must implement the specific logic for automatic decision-making.
     */
    @Transactional
    public abstract ProposalDecisionAttempt makeAutomaticDecision(T proposal);

    /**
     * Creates a manual decision for a proposal.
     */
    @Transactional
    public ProposalDecisionAttempt makeManualDecision(Long proposalId, DecisionOutcome outcome, String notes, User moderator) {
        log.info("Creating manual decision for proposal ID: {}, Outcome: {}, Moderator ID: {}", proposalId, outcome, moderator.getId());
        T proposal = getProposalOrThrow(proposalId);

        validateManualDecision(proposal, outcome);

        ProposalDecisionAttempt attempt = ProposalDecisionAttempt.builder()
                .proposal(proposal)
                .type(DecisionType.MANUAL)
                .outcome(outcome)
                .confident(true)
                .notes(notes)
                .decidedAt(Instant.now())
                .decidedBy(moderator)
                .build();

        return saveAndApplyDecision(proposal, attempt);
    }

    /**
     * Optional validation hook for manual decisions.
     */
    protected void validateManualDecision(T proposal, DecisionOutcome outcome) {
        // Default implementation does nothing
    }

    /**
     * Activates a specific decision attempt for a proposal.
     */
    @Transactional
    public void activateDecision(Long attemptId) {
        ProposalDecisionAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision attempt not found with id: " + attemptId));
        
        if (!proposalType.isInstance(attempt.getProposal())) {
            throw new IllegalArgumentException("Decision attempt " + attemptId + " does not belong to a proposal of type " + proposalType.getSimpleName());
        }

        T proposal = proposalType.cast(attempt.getProposal());
        log.info("Activating decision attempt ID: {} for proposal ID: {}", attemptId, proposal.getId());

        saveAndApplyDecision(proposal, attempt);
    }

    /**
     * Deactivates the current decision for a proposal, reverting it to UNDER_REVIEW status.
     */
    @Transactional
    public void deactivateDecision(Long proposalId) {
        T proposal = getProposalOrThrow(proposalId);
        log.info("Deactivating decision for proposal ID: {}", proposalId);

        if (proposal.getStatus() == ProposalStatus.UNDER_REVIEW || proposal.getStatus() == ProposalStatus.SUBMITTED) {
            log.warn("Proposal ID {} has no active decision to deactivate (status: {})", proposalId, proposal.getStatus());
            return;
        }

        proposal.setStatus(ProposalStatus.UNDER_REVIEW);
        proposalRepo.save(proposal);

        log.info("Successfully deactivated decision for proposal ID: {}. Status reverted to UNDER_REVIEW", proposalId);
    }

    // ==== Helper Methods ====

    protected ProposalDecisionAttempt saveAndApplyDecision(T proposal, ProposalDecisionAttempt attempt) {
        attemptRepo.save(attempt);
        log.debug("Saved decision attempt with ID: {}", attempt.getId());

        applyDecisionToProposal(proposal, attempt);
        proposalRepo.save(proposal);
        log.info("Updated proposal ID: {} status to: {}", proposal.getId(), proposal.getStatus());

        if (attempt.getOutcome() == DecisionOutcome.ACCEPT) {
            publishAcceptedEvent(proposal);
        }

        return attempt;
    }

    protected abstract void publishAcceptedEvent(T proposal);

    private void applyDecisionToProposal(T proposal, ProposalDecisionAttempt decision) {
        if (decision.getType() == DecisionType.MANUAL) {
            proposal.setStatus(decision.getOutcome() == DecisionOutcome.ACCEPT
                    ? ProposalStatus.MANUALLY_ACCEPTED
                    : ProposalStatus.MANUALLY_REJECTED);
        } else {
            // Automatic
            if (decision.getOutcome() == DecisionOutcome.ACCEPT) {
                proposal.setStatus(ProposalStatus.AUTO_ACCEPTED);
            } else if (decision.getOutcome() == DecisionOutcome.REJECT) {
                proposal.setStatus(ProposalStatus.AUTO_REJECTED);
            } else {
                // Inconclusive
                proposal.setStatus(ProposalStatus.UNDER_REVIEW);
            }
        }
    }

    protected T getProposalOrThrow(Long id) {
        return proposalRepo.findById(id)
                .orElseThrow(() -> {
                    log.error("Proposal with ID {} not found", id);
                    return new ResourceNotFoundException("Proposal not found with id: " + id);
                });
    }
}
