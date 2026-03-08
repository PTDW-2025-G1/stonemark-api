package pt.estga.decision.rules;

import pt.estga.submission.entities.MarkOccurrenceSubmission;

/**
 * Represents a single rule for evaluating a submission.
 */
public interface DecisionRule {

    /**
     * Evaluates the submission against this rule.
     *
     * @param proposal The submission to evaluate.
     * @return A DecisionRuleResult containing the outcome if the rule matches, or empty if it doesn't.
     */
    DecisionRuleResult evaluate(MarkOccurrenceSubmission proposal);

    /**
     * Defines the order in which rules should be applied.
     * Lower values run first.
     *
     * @return The order value.
     */
    int getOrder();
}
