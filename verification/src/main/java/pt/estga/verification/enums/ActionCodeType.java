package pt.estga.verification.enums;

/**
 * Types of action codes used in the verification system.
 * Currently only supports chatbot verification for linking Telegram accounts.
 */
public enum ActionCodeType {
    /**
     * Verification code for linking chatbot accounts (Telegram).
     * Generated in chatbot, verified in frontend/backend.
     */
    CHATBOT_VERIFICATION
}
