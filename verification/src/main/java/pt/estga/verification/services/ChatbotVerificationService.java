package pt.estga.verification.services;

import pt.estga.verification.entities.ActionCode;

import java.util.Optional;

public interface ChatbotVerificationService {

    /**
     * Generate a verification code for a Telegram user (by telegram ID).
     * This code will be shown in the chatbot and entered in the frontend.
     */
    ActionCode generateChatbotVerificationCode(String telegramId);

    /**
     * Verify a code entered in the frontend and return the associated Telegram ID.
     * Returns empty if code is invalid, expired, or already consumed.
     */
    Optional<String> verifyAndGetTelegramId(String code);

}
