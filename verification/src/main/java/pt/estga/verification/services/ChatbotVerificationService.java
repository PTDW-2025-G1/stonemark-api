package pt.estga.verification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.repositories.ActionCodeRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotVerificationService {

    private final ActionCodeRepository actionCodeRepository;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;

    /**
     * Generate a verification code for a Telegram user (by telegram ID).
     * This code will be shown in the chatbot and entered in the frontend.
     */
    @Transactional
    public ActionCode generateChatbotVerificationCode(String platformUserId) {
        log.info("Generating chatbot verification code for platform user: {}", platformUserId);

        // Invalidate existing codes for this platform user ID and type
        actionCodeRepository.deleteByPlatformUserIdAndType(platformUserId, ActionCodeType.CHATBOT_VERIFICATION);

        String code = generateRandomCode();
        ActionCode actionCode = ActionCode.builder()
                .code(code)
                .platformUserId(platformUserId)
                .type(ActionCodeType.CHATBOT_VERIFICATION)
                .expiresAt(Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .consumed(false)
                .build();

        return actionCodeRepository.save(actionCode);
    }

    /**
     * Verify a code entered in the frontend and return the associated Telegram ID.
     * Returns empty if code is invalid, expired, or already consumed.
     */
    @Transactional
    public Optional<String> verifyAndGetPlatformUserId(String code) {
        log.info("Verifying chatbot code: {}", code);

        Optional<ActionCode> actionCodeOptional = actionCodeRepository.findByCode(code);

        if (actionCodeOptional.isEmpty()) {
            log.warn("Code not found: {}", code);
            return Optional.empty();
        }

        ActionCode actionCode = actionCodeOptional.get();

        if (actionCode.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Code expired: {}", code);
            return Optional.empty();
        }

        if (actionCode.getType() != ActionCodeType.CHATBOT_VERIFICATION) {
            log.warn("Invalid code type: {}", actionCode.getType());
            return Optional.empty();
        }

        // Atomically mark as consumed — prevents race where two requests both pass validation
        int updated = actionCodeRepository.markConsumed(code);
        if (updated == 0) {
            log.warn("Code already consumed (concurrent request): {}", code);
            return Optional.empty();
        }

        String platformUserId = actionCode.getPlatformUserId();
        log.info("Chatbot verification successful for platform user: {}", platformUserId);
        return Optional.of(platformUserId);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredCodes() {
        int deleted = actionCodeRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired chatbot verification codes", deleted);
        }
    }

    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        return code.toString();
    }
}
