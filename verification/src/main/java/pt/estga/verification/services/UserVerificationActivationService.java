package pt.estga.verification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.models.Email;
import pt.estga.shared.services.EmailService;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVerificationActivationService {

    private final ActionCodeService actionCodeService;
    private final UserService userService;
    private final EmailService emailService;

    @Transactional
    public Optional<String> activateContactVerification(ActionCode actionCode) {
        log.info("Activating user verification flags for action code: {}", actionCode.getCode());

        User user = actionCode.getUser();
        if (user == null) {
            throw new IllegalStateException("ActionCode has no associated User.");
        }

        if (actionCode.getType() == ActionCodeType.EMAIL_VERIFICATION) {
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                userService.update(user);
            }
            actionCodeService.consumeCode(actionCode);
            sendConfirmationEmail(user);
            return Optional.empty();
        }

        if (actionCode.getType() == ActionCodeType.PHONE_VERIFICATION) {
            if (!user.isPhoneVerified()) {
                user.setPhoneVerified(true);
                userService.update(user);
            }
            actionCodeService.consumeCode(actionCode);
            return Optional.empty();
        }

        throw new IllegalArgumentException("Unsupported action code type for contact activation: " + actionCode.getType());
    }

    private void sendConfirmationEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        Email email = Email.builder()
                .to(user.getEmail())
                .subject("Email Address Verified")
                .template("email/email-verification-confirmation")
                .properties(Map.of("name", user.getFirstName() == null ? "User" : user.getFirstName(), "verifiedAt", Instant.now().toString()))
                .build();
        emailService.sendEmail(email);
        log.info("Sent verification confirmation email to {}", user.getEmail());
    }
}

