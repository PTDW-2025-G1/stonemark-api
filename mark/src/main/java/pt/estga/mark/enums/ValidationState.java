package pt.estga.mark.enums;

/**
 * Mark lifecycle status. PROVISIONAL is used for discovery-created marks that
 * require later validation/resolution.
 */
public enum ValidationState {
    VERIFIED,
    PROVISIONAL,
    REJECTED
}
