package pt.estga.review.enums;

/**
 * Describes the kind of review being performed. Using a typed enum makes the
 * review template easier to reason about than a control boolean.
 */
public enum ReviewType {
    /** The submission matches an existing mark. */
    MATCH,
    /** The submission matches no existing mark; it becomes a new-mark candidate. */
    NO_MATCH,
    /** The submission is not a mark. */
    REJECTION
}
