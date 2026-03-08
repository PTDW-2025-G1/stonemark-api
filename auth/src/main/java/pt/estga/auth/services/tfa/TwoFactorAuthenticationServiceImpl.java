package pt.estga.auth.services.tfa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.exceptions.InvalidTokenException;
import pt.estga.shared.models.Email;
import pt.estga.shared.services.EmailService;
import pt.estga.shared.services.SmsService;
import pt.estga.user.entities.User;
import pt.estga.user.enums.TfaMethod;
import pt.estga.user.services.UserService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.repositories.ActionCodeRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthenticationServiceImpl implements TwoFactorAuthenticationService {

    private final SmsService smsService;
    private final EmailService emailService;
    private final ActionCodeRepository actionCodeRepository;
    private final UserService userService;
    private final TotpService totpService;

    @Value("${application.security.tfa.code-expiration-minutes:5}")
    private long codeExpirationMinutes;

    @Override
    @Transactional
    public void generateAndSendSmsCode(User user) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new IllegalStateException("User has no phone for SMS 2FA.");
        }

        String code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        saveActionCode(user, code);
        smsService.sendMessage(user.getPhone(), "Your 2FA code is: " + code);
    }

    @Override
    @Transactional
    public void generateAndSendEmailCode(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalStateException("User has no email for Email 2FA.");
        }

        String code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        saveActionCode(user, code);

        Map<String, Object> properties = new HashMap<>();
        properties.put("code", code);
        emailService.sendEmail(Email.builder()
                .to(user.getEmail())
                .subject("Two-Factor Authentication Code")
                .template("email/tfa-code.html")
                .properties(properties)
                .build());
    }

    @Override
    @Transactional
    public boolean verifyCode(User user, String code, ActionCodeType type) {
        ActionCode actionCode = actionCodeRepository.findByUserAndType(user, type)
                .orElseThrow(() -> new InvalidTokenException("2FA code not found or expired."));

        if (actionCode.getExpiresAt().isBefore(Instant.now())) {
            actionCodeRepository.delete(actionCode);
            throw new InvalidTokenException("2FA code expired.");
        }

        if (actionCode.getCode().equals(code)) {
            actionCodeRepository.delete(actionCode); // Code used, delete it
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void requestTfaContactCode(User user) {
        if (user.getTfaMethod() == TfaMethod.SMS) {
            generateAndSendSmsCode(user);
        } else if (user.getTfaMethod() == TfaMethod.EMAIL) {
            generateAndSendEmailCode(user);
        } else {
            throw new IllegalStateException("Contact-based 2FA is not enabled for this user.");
        }
    }

    @Override
    @Transactional
    public boolean verifyTfaContactCode(User user, String code) {
        if (user.getTfaMethod() == TfaMethod.SMS || user.getTfaMethod() == TfaMethod.EMAIL) {
            return verifyCode(user, code, ActionCodeType.TWO_FACTOR);
        }
        return false;
    }

    @Override
    @Transactional
    public void setTfaMethod(User user, TfaMethod method) {
        if (user.getTfaMethod() == TfaMethod.TOTP && method != TfaMethod.TOTP) {
            totpService.disableTfa(user);
        }

        user.setTfaMethod(method);
        if (method == TfaMethod.TOTP) {
            totpService.enableTfa(user, method);
        } else if (method == TfaMethod.NONE) {
            totpService.disableTfa(user);
        }
        userService.update(user);
    }

    @Override
    @Transactional
    public boolean verifyAndDisableCurrentTfa(User user, String code) {
        log.info("Attempting to verify and disable TFA for user: {}", user.getUsername());
        boolean isValid = false;
        TfaMethod tfaMethod = user.getTfaMethod();
        log.debug("User {} current TFA method is: {}", user.getUsername(), tfaMethod);

        if (tfaMethod == TfaMethod.TOTP) {
            if (user.getTfaSecret() != null) {
                isValid = totpService.isCodeValid(user.getTfaSecret(), code);
                log.debug("TOTP code verification result for user {}: {}", user.getUsername(), isValid);
            } else {
                log.warn("User {} has TOTP method set but no secret is stored.", user.getUsername());
            }
        } else if (tfaMethod == TfaMethod.SMS || tfaMethod == TfaMethod.EMAIL) {
            isValid = verifyTfaContactCode(user, code);
            log.debug("Contact-based TFA code verification result for user {}: {}", user.getUsername(), isValid);
        } else {
            log.warn("User {} has an unsupported TFA method for verification: {}", user.getUsername(), tfaMethod);
        }

        if (isValid) {
            log.info("TFA code verified successfully for user: {}. Disabling TFA.", user.getUsername());
            totpService.disableTfa(user); // This method now handles setting TfaMethod.NONE and clearing secret
        } else {
            log.warn("TFA code verification failed for user: {}", user.getUsername());
        }
        return isValid;
    }

    private void saveActionCode(User user, String code) {
        // Delete any existing code for this purpose
        actionCodeRepository.deleteByUserAndType(user, ActionCodeType.TWO_FACTOR);

        ActionCode actionCode = ActionCode.builder()
                .user(user)
                .code(code)
                .type(ActionCodeType.TWO_FACTOR)
                .expiresAt(Instant.now().plusSeconds(codeExpirationMinutes * 60))
                .build();
        actionCodeRepository.save(actionCode);
    }
}
