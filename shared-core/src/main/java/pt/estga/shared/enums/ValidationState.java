package pt.estga.shared.enums;

/**
 * Centralized validation state used across multiple modules.
 */
public enum ValidationState {
    VERIFIED,    // Expert-confirmed data
    PROVISIONAL, // User-submitted or discovery data
    REJECTED     // Flagged as invalid/incorrect
}
