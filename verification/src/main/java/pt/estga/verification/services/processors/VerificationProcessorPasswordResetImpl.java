package pt.estga.verification.services.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.estga.shared.models.Email;
import pt.estga.shared.services.EmailService;
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
    public Optional<String> process(String recipient, ActionCode code) {

        String token = code.getCode();
        String resetLink = resetPasswordUrl + "?token=" + token;

        long expirationMinutes = Duration.between(
                Instant.now(),
                code.getExpiresAt()
        ).toMinutes();

        String to = (recipient != null && !recipient.isBlank())
                ? recipient
                : (code.getRecipient() != null && !code.getRecipient().isBlank())
                ? code.getRecipient()
                : code.getUser() != null ? code.getUser().getEmail() : null;

        if (to == null || to.isBlank()) {
            return Optional.of(token);
        }

        Email email = Email.builder()
                .to(to)
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