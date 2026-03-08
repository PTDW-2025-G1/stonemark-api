package pt.estga.decision.rules.markoccurrence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.decision.enums.DecisionOutcome;
import pt.estga.decision.rules.DecisionRule;
import pt.estga.decision.rules.DecisionRuleResult;
import pt.estga.submission.config.SubmissionDecisionProperties;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

/**
 * Decision rule that handles proposals for new monuments/marks.
 * New monument proposals typically require manual review unless they meet
 * very high quality standards.
 */
@Component
@RequiredArgsConstructor
public class NewMonumentRule implements DecisionRule {

    private final SubmissionDecisionProperties properties;

    // Higher threshold for new monuments
    private static final int NEW_MONUMENT_ACCEPTANCE_THRESHOLD = 80;

    @Override
    public DecisionRuleResult evaluate(MarkOccurrenceSubmission proposal) {
        // Check if this is a new monument/mark submission
        boolean isNewMonument = proposal.getExistingMonument() == null;
        boolean isNewMark = proposal.isNewMark();

        if (!isNewMonument && !isNewMark) {
            return null; // This rule only applies to new monuments/marks
        }

        // If configuration requires manual review for new monuments, route to review
        if (properties.getRequireManualReviewForNewMonuments()) {
            Integer credibilityScore = proposal.getCredibilityScore();

            // Only auto-accept new monuments with very high credibility and complete data
            if (credibilityScore != null && credibilityScore >= NEW_MONUMENT_ACCEPTANCE_THRESHOLD) {
                boolean hasAllData = proposal.getLatitude() != null
                    && proposal.getLongitude() != null
                    && proposal.getOriginalMediaFile() != null
                    && proposal.getUserNotes() != null
                    && !proposal.getUserNotes().isEmpty();

                if (hasAllData) {
                    return DecisionRuleResult.conclusive(
                            DecisionOutcome.ACCEPT,
                            true,
                            "New monument/mark with exceptional quality (credibility: " + credibilityScore + ") and complete data."
                    );
                }
            }

            // Otherwise, send to manual review
            return DecisionRuleResult.conclusive(
                    DecisionOutcome.INCONCLUSIVE,
                    true,
                    "New monument/mark requires manual review per policy."
            );
        }

        return null; // Let other rules decide if manual review is not required
    }

    @Override
    public int getOrder() {
        return 10; // Run first to catch new monuments early
    }
}

