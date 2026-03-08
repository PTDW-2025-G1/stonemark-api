package pt.estga.decision.rules;

import pt.estga.submission.entities.Submission;

/**
 * Represents a single rule for evaluating a submission.
 *
 * @param <T> The type of submission this rule applies to.
 */
public interface DecisionRule<T extends Submission> {

    /**
     * Evaluates the submission against this rule.
     *
     * @param proposal The submission to evaluate.
     * @return A DecisionRuleResult containing the outcome if the rule matches, or empty if it doesn't.
     */
    DecisionRuleResult evaluate(T proposal);

    /**
     * Defines the order in which rules should be applied.
     * Lower values run first.
     *
     * @return The order value.
     */
    int getOrder();
}
