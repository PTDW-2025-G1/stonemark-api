package pt.estga.verification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.estga.user.entities.User;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.repositories.ActionCodeRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionCodeServiceImpl implements ActionCodeService {

    private final ActionCodeRepository actionCodeRepository;

    @Value("${application.security.action-code.email-verification.expiration}")
    private long emailVerificationExpiration;

    @Value("${application.security.action-code.password-reset.expiration}")
    private long passwordResetExpiration;

    @Value("${application.security.action-code.two-factor.expiration}")
    private long twoFactorExpiration;

    @Value("${application.security.action-code.telephone-verification.expiration}")
    private long telephoneVerificationExpiration;

    @Value("${application.security.action-code.device-verification.expiration}")
    private long deviceVerificationExpiration;

    @Value("${application.security.action-code.chatbot-verification.expiration}")
    private long chatbotVerificationExpiration;

    @Override
    public ActionCode createAndSave(User user, String recipient, ActionCodeType type) {
        log.info("Creating and saving action code of type {} for user {}", type, user.getId());
        try {
            // Invalidate existing codes of the same type for this user
            actionCodeRepository.findByUserAndType(user, type).ifPresent(actionCodeRepository::delete);

            long expirationMillis = getExpirationMillisFor(type);

            String code = RandomStringUtils.secure().nextAlphanumeric(6).toUpperCase();

            ActionCode actionCode = ActionCode.builder()
                    .code(code)
                    .user(user)
                    .recipient(recipient)
                    .type(type)
                    .expiresAt(Instant.now().plusMillis(expirationMillis))
                    .consumed(false)
                    .build();

            ActionCode savedActionCode = actionCodeRepository.save(actionCode);
            log.info("Successfully created and saved action code with id {}", savedActionCode.getId());
            return savedActionCode;
        } catch (Exception e) {
            log.error("Error creating and saving action code for user {}", user.getId(), e);
            throw e;
        }
    }

    @Override
    public Optional<ActionCode> findByCode(String code) {
        return actionCodeRepository.findAll().stream()
                .filter(ac -> ac.getCode().equals(code) && !ac.isConsumed())
                .findFirst();
    }

    @Override
    public boolean isCodeValid(String code) {
        return findByCode(code)
                .map(ac -> !ac.isConsumed() && ac.getExpiresAt().isAfter(Instant.now()))
                .orElse(false);
    }

    @Override
    public void consumeCode(ActionCode code) {
        code.setConsumed(true);
        actionCodeRepository.save(code);
    }

    private long getExpirationMillisFor(ActionCodeType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> emailVerificationExpiration;
            case PHONE_VERIFICATION -> telephoneVerificationExpiration;
            case TWO_FACTOR -> twoFactorExpiration;
            case DEVICE_VERIFICATION -> deviceVerificationExpiration;
            case TELEGRAM_VERIFICATION, CHATBOT_VERIFICATION -> chatbotVerificationExpiration;
        };
    }
}
