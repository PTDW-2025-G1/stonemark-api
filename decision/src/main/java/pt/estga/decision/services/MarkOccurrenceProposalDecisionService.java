package pt.estga.decision.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.decision.entities.ProposalDecisionAttempt;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.enums.DecisionType;
import pt.estga.decision.repositories.ProposalDecisionAttemptRepository;
import pt.estga.decision.rules.DecisionRule;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.events.ProposalAcceptedEvent;
import pt.estga.submission.repositories.MarkOccurrenceProposalRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class MarkOccurrenceProposalDecisionService extends ProposalDecisionService<MarkOccurrenceSubmission> {

    private final List<DecisionRule<MarkOccurrenceSubmission>> rules;

    public MarkOccurrenceProposalDecisionService(
            ProposalDecisionAttemptRepository attemptRepo,
            MarkOccurrenceProposalRepository proposalRepo,
            ApplicationEventPublisher eventPublisher,
            List<DecisionRule<MarkOccurrenceSubmission>> rules
    ) {
        super(attemptRepo, proposalRepo, eventPublisher, MarkOccurrenceSubmission.class);
        this.rules = rules;
    }

    /**
     * Automatically evaluates a submission based on a set of injected rules.
     * <p>
     * The rules are evaluated in order (defined by {@link DecisionRule#getOrder()}).
     * The first rule to return a conclusive result determines the outcome.
     * If no rule matches, the outcome defaults to INCONCLUSIVE.
     *
     * @param proposal The submission to evaluate.
     * @return The created decision attempt.
     */
    @Override
    @Transactional
    public ProposalDecisionAttempt makeAutomaticDecision(MarkOccurrenceSubmission proposal) {
        log.debug("Running automatic decision logic for submission ID: {}, Priority: {}", proposal.getId(), proposal.getPriority());

        DecisionOutcome outcome = DecisionOutcome.INCONCLUSIVE;
        var confident = false;
        var notes = "No rule matched; defaulting to inconclusive.";

        // Sort rules by order
        var sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(DecisionRule::getOrder))
                .toList();

        for (var rule : sortedRules) {
            var result = rule.evaluate(proposal);
            if (result != null) {
                outcome = result.outcome();
                confident = result.confident();
                notes = result.reason();
                log.info("Rule '{}' matched for submission ID {}: {}", rule.getClass().getSimpleName(), proposal.getId(), result);
                break;
            }
        }

        log.info("Automatic decision outcome for submission ID {}: {} (Confident: {})", proposal.getId(), outcome, confident);

        var attempt = ProposalDecisionAttempt.builder()
                .submission(proposal)
                .type(DecisionType.AUTOMATIC)
                .outcome(outcome)
                .confident(confident)
                .notes(notes)
                .decidedAt(Instant.now())
                .build();

        return saveAndApplyDecision(proposal, attempt);
    }

    @Override
    protected void validateManualDecision(MarkOccurrenceSubmission proposal, DecisionOutcome outcome) {
    }

    @Override
    protected void publishAcceptedEvent(MarkOccurrenceSubmission proposal) {
        eventPublisher.publishEvent(new ProposalAcceptedEvent(this, proposal));
    }
}
