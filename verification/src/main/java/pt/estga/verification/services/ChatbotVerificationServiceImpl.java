package pt.estga.verification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class ChatbotVerificationServiceImpl implements ChatbotVerificationService {

    private final ActionCodeRepository actionCodeRepository;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;

    @Override
    @Transactional
    public ActionCode generateChatbotVerificationCode(String telegramId) {
        log.info("Generating chatbot verification code for Telegram user: {}", telegramId);

        // Invalidate existing codes for this telegram ID and type
        actionCodeRepository.deleteByTelegramIdAndType(telegramId, ActionCodeType.CHATBOT_VERIFICATION);

        String code = generateRandomCode();
        ActionCode actionCode = ActionCode.builder()
                .code(code)
                .telegramId(telegramId)
                .type(ActionCodeType.CHATBOT_VERIFICATION)
                .expiresAt(Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .consumed(false)
                .build();

        return actionCodeRepository.save(actionCode);
    }

    @Override
    @Transactional
    public Optional<String> verifyAndGetTelegramId(String code) {
        log.info("Verifying chatbot code: {}", code);

        Optional<ActionCode> actionCodeOptional = actionCodeRepository.findByCode(code);

        if (actionCodeOptional.isEmpty()) {
            log.warn("Code not found: {}", code);
            return Optional.empty();
        }

        ActionCode actionCode = actionCodeOptional.get();

        if (actionCode.isConsumed()) {
            log.warn("Code already consumed: {}", code);
            return Optional.empty();
        }

        if (actionCode.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Code expired: {}", code);
            return Optional.empty();
        }

        if (actionCode.getType() != ActionCodeType.CHATBOT_VERIFICATION) {
            log.warn("Invalid code type: {}", actionCode.getType());
            return Optional.empty();
        }

        String telegramId = actionCode.getTelegramId();

        // Mark code as consumed
        actionCode.setConsumed(true);
        actionCodeRepository.save(actionCode);

        log.info("Chatbot verification successful for Telegram user: {}", telegramId);
        return Optional.of(telegramId);
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
