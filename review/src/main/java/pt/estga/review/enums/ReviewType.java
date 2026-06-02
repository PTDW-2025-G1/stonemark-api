package pt.estga.review.enums;

/**
 * Describes the kind of review being performed. Using a typed enum makes the
 * processReview template easier to reason about than a control boolean.
 */
public enum ReviewType {
    MATCH,
    DISCOVERY,
    REJECTION
}
