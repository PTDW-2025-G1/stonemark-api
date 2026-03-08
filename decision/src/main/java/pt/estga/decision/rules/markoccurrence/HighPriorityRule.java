package pt.estga.decision.rules.markoccurrence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.rules.DecisionRule;
import pt.estga.decision.rules.DecisionRuleResult;
import pt.estga.submission.config.SubmissionDecisionProperties;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

@Component
@RequiredArgsConstructor
public class HighPriorityRule implements DecisionRule {

    private final SubmissionDecisionProperties properties;

    @Override
    public DecisionRuleResult evaluate(MarkOccurrenceSubmission proposal) {
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
