package pt.estga.decision.rules.markoccurrence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.rules.DecisionRule;
import pt.estga.decision.rules.DecisionRuleResult;
import pt.estga.proposal.config.ProposalDecisionProperties;
import pt.estga.proposal.entities.MarkOccurrenceProposal;

@Component
@RequiredArgsConstructor
public class HighPriorityRule implements DecisionRule<MarkOccurrenceProposal> {

    private final ProposalDecisionProperties properties;

    @Override
    public DecisionRuleResult evaluate(MarkOccurrenceProposal proposal) {
        Integer priority = proposal.getPriority();

        if (priority == null) {
            return null; // Skip if no priority calculated
        }

        if (priority > properties.getAutomaticAcceptanceThreshold()) {
            return DecisionRuleResult.conclusive(
                    DecisionOutcome.ACCEPT,
                    true,
                    "Priority " + priority + " exceeds acceptance threshold."
            );
        }

        return null;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
