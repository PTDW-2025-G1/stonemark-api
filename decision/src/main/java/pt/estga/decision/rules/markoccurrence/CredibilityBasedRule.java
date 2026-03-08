package pt.estga.decision.rules.markoccurrence;

import org.springframework.stereotype.Component;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.rules.DecisionRule;
import pt.estga.decision.rules.DecisionRuleResult;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

/**
 * Decision rule that evaluates proposals based on their credibility score.
 * This rule runs before the priority-based rules and provides a middle ground
 * for proposals with good data quality and user reputation.
 */
@Component
public class CredibilityBasedRule implements DecisionRule<MarkOccurrenceSubmission> {

    // Credibility thresholds for automatic decisions
    private static final int HIGH_CREDIBILITY_THRESHOLD = 60;  // 60% credibility for auto-acceptance
    private static final int LOW_CREDIBILITY_THRESHOLD = 20;   // Below 20% auto-reject

    @Override
    public DecisionRuleResult evaluate(MarkOccurrenceSubmission proposal) {
        Integer credibilityScore = proposal.getCredibilityScore();

        if (credibilityScore == null) {
            return null; // Skip if no credibility score available
        }

        // High credibility proposals with complete data can be auto-accepted
        if (credibilityScore >= HIGH_CREDIBILITY_THRESHOLD) {
            // Additional checks for high-confidence acceptance
            boolean hasLocation = proposal.getLatitude() != null && proposal.getLongitude() != null;
            boolean hasMedia = proposal.getOriginalMediaFile() != null;
            boolean hasNotes = proposal.getUserNotes() != null && !proposal.getUserNotes().isEmpty();

            // Require at least location + (media OR notes) for auto-acceptance
            if (hasLocation && (hasMedia || hasNotes)) {
                return DecisionRuleResult.conclusive(
                        DecisionOutcome.ACCEPT,
                        true,
                        "Credibility score " + credibilityScore + " with complete data meets acceptance criteria."
                );
            }
        }

        // Very low credibility proposals can be rejected
        if (credibilityScore < LOW_CREDIBILITY_THRESHOLD) {
            return DecisionRuleResult.conclusive(
                    DecisionOutcome.REJECT,
                    false, // Not fully confident, might want manual review
                    "Credibility score " + credibilityScore + " is too low."
            );
        }

        return null; // No match, let other rules decide
    }

    @Override
    public int getOrder() {
        return 15; // Run before HighPriorityRule (20) but after any critical validation rules
    }
}


