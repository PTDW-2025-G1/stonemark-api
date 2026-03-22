package pt.estga.decision.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.decision.entities.SubmissionDecisionAttempt;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.enums.DecisionType;
import pt.estga.decision.repositories.SubmissionDecisionAttemptRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.user.entities.User;

import java.time.Instant;

@Slf4j
public abstract class SubmissionDecisionService {

    protected final SubmissionDecisionAttemptRepository attemptRepo;
    protected final MarkOccurrenceSubmissionRepository proposalRepo;
    protected final ApplicationEventPublisher eventPublisher;

    protected SubmissionDecisionService(
            SubmissionDecisionAttemptRepository attemptRepo,
            MarkOccurrenceSubmissionRepository proposalRepo,
            ApplicationEventPublisher eventPublisher
    ) {
        this.attemptRepo = attemptRepo;
        this.proposalRepo = proposalRepo;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Triggers the automatic decision logic for a submission by ID.
     */
    @Transactional
    public SubmissionDecisionAttempt makeAutomaticDecision(Long proposalId) {
        log.info("Processing automatic decision for submission ID: {}", proposalId);
        MarkOccurrenceSubmission proposal = getProposalOrThrow(proposalId);
        return makeAutomaticDecision(proposal);
    }

    /**
     * Triggers the automatic decision logic for a submission entity.
     * Subclasses must implement the specific logic for automatic decision-making.
     */
    @Transactional
    public abstract SubmissionDecisionAttempt makeAutomaticDecision(MarkOccurrenceSubmission proposal);

    /**
     * Creates a manual decision for a submission.
     */
    @Transactional
    public SubmissionDecisionAttempt makeManualDecision(Long proposalId, DecisionOutcome outcome, String notes, User moderator) {
        log.info("Creating manual decision for submission ID: {}, Outcome: {}, Moderator ID: {}", proposalId, outcome, moderator.getId());
        MarkOccurrenceSubmission proposal = getProposalOrThrow(proposalId);

        validateManualDecision(proposal, outcome);

        SubmissionDecisionAttempt attempt = SubmissionDecisionAttempt.builder()
                .submission(proposal)
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
    protected void validateManualDecision(MarkOccurrenceSubmission proposal, DecisionOutcome outcome) {
        // Default implementation does nothing
    }

    /**
     * Activates a specific decision attempt for a submission.
     */
    @Transactional
    public void activateDecision(Long attemptId) {
        SubmissionDecisionAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision attempt not found with id: " + attemptId));
        
        MarkOccurrenceSubmission proposal = attempt.getSubmission();
        log.info("Activating decision attempt ID: {} for submission ID: {}", attemptId, proposal.getId());

        saveAndApplyDecision(proposal, attempt);
    }

    /**
     * Deactivates the current decision for a submission, reverting it to UNDER_REVIEW status.
     */
    @Transactional
    public void deactivateDecision(Long proposalId) {
        MarkOccurrenceSubmission proposal = getProposalOrThrow(proposalId);
        log.info("Deactivating decision for submission ID: {}", proposalId);

        if (proposal.getStatus() == SubmissionStatus.UNDER_REVIEW || proposal.getStatus() == SubmissionStatus.SUBMITTED) {
            log.warn("Submission ID {} has no active decision to deactivate (status: {})", proposalId, proposal.getStatus());
            return;
        }

        proposal.setStatus(SubmissionStatus.UNDER_REVIEW);
        proposalRepo.save(proposal);

        log.info("Successfully deactivated decision for submission ID: {}. Status reverted to UNDER_REVIEW", proposalId);
    }

    // ==== Helper Methods ====

    protected SubmissionDecisionAttempt saveAndApplyDecision(MarkOccurrenceSubmission proposal, SubmissionDecisionAttempt attempt) {
        attemptRepo.save(attempt);
        log.debug("Saved decision attempt with ID: {}", attempt.getId());

        applyDecisionToProposal(proposal, attempt);
        proposalRepo.save(proposal);
        log.info("Updated submission ID: {} status to: {}", proposal.getId(), proposal.getStatus());

        if (attempt.getOutcome() == DecisionOutcome.ACCEPT) {
            publishAcceptedEvent(proposal);
        }

        return attempt;
    }

    protected abstract void publishAcceptedEvent(MarkOccurrenceSubmission proposal);

    private void applyDecisionToProposal(MarkOccurrenceSubmission proposal, SubmissionDecisionAttempt decision) {
        if (decision.getType() == DecisionType.MANUAL) {
            proposal.setStatus(decision.getOutcome() == DecisionOutcome.ACCEPT
                    ? SubmissionStatus.MANUALLY_ACCEPTED
                    : SubmissionStatus.MANUALLY_REJECTED);
        } else {
            // Automatic
            if (decision.getOutcome() == DecisionOutcome.ACCEPT) {
                proposal.setStatus(SubmissionStatus.AUTO_ACCEPTED);
            } else if (decision.getOutcome() == DecisionOutcome.REJECT) {
                proposal.setStatus(SubmissionStatus.AUTO_REJECTED);
            } else {
                // Inconclusive
                proposal.setStatus(SubmissionStatus.UNDER_REVIEW);
            }
        }
    }

    protected MarkOccurrenceSubmission getProposalOrThrow(Long id) {
        return proposalRepo.findById(id)
                .orElseThrow(() -> {
                    log.error("Submission with ID {} not found", id);
                    return new ResourceNotFoundException("Submission not found with id: " + id);
                });
    }
}
