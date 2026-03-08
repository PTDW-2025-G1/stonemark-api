package pt.estga.verification.services.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.estga.shared.models.Email;
import pt.estga.shared.services.EmailService;
import pt.estga.user.entities.UserContact;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VerificationProcessorPasswordResetImpl implements VerificationProcessor {

    private final EmailService emailService;

    @Value("${application.frontend.auth-url}/reset-password")
    private String resetPasswordUrl;

    @Override
    public Optional<String> process(UserContact userContact, ActionCode code) {

        String token = code.getCode();
        String resetLink = resetPasswordUrl + "?token=" + token;

        long expirationMinutes = Duration.between(
                Instant.now(),
                code.getExpiresAt()
        ).toMinutes();

        String recipient = userContact != null
                ? userContact.getValue()
                : code.getUser() != null ? code.getUser().getEmail() : null;

        if (recipient == null || recipient.isBlank()) {
            return Optional.of(token);
        }

        Email email = Email.builder()
                .to(recipient)
                .subject("Reset your password")
                .template("email/password-reset")
                .properties(Map.of(
                        "name", code.getUser().getFirstName(),
                        "code", token,
                        "link", resetLink,
                        "expiration", expirationMinutes
                ))
                .build();

        emailService.sendEmail(email);

        return Optional.of(token);
    }

    @Override
    public ActionCodeType getType() {
        return ActionCodeType.RESET_PASSWORD;
    }
}