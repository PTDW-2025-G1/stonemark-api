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
public class LowPriorityRule implements DecisionRule<MarkOccurrenceSubmission> {

    private final SubmissionDecisionProperties properties;

    @Override
    public DecisionRuleResult evaluate(MarkOccurrenceSubmission proposal) {
        Integer priority = proposal.getPriority();

        if (priority == null) {
            return null; // Skip if no priority calculated
        }

        // Only reject if priority is very low AND credibility is also low
        if (priority < properties.getAutomaticRejectionThreshold()) {
            Integer credibilityScore = proposal.getCredibilityScore();

            // Double-check credibility before rejecting
            // Only auto-reject if both priority AND credibility are low
            if (credibilityScore != null && credibilityScore < 15) {
                return DecisionRuleResult.conclusive(
                        DecisionOutcome.REJECT,
                        false,
                        "Priority " + priority + " and credibility " + credibilityScore + " are both below thresholds."
                );
            }

            // If credibility is acceptable but priority is low, send to manual review
            return DecisionRuleResult.conclusive(
                    DecisionOutcome.INCONCLUSIVE,
                    false,
                    "Priority " + priority + " is low, requires manual review."
            );
        }

        return null;
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
