package pt.estga.verification.enums;

/**
 * Types of action codes used in the verification system.
 * Currently, supports chatbot account linking.
 */
public enum ActionCodeType {
    /**
     * Verification code for linking chatbot accounts (platform-agnostic).
     * Generated in chatbot, verified in frontend/backend.
     */
    CHATBOT_VERIFICATION
}
