package pt.estga.verification.services.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.UserActivationService;

import java.util.Optional;

/**
 * Processor for handling {@link ActionCodeType#PHONE_VERIFICATION}.
 * This component delegates the actual user enabling logic to {@link UserActivationService}.
 */
@Component
@RequiredArgsConstructor
public class VerificationProcessorTelephoneImpl implements VerificationProcessor {

    private final UserActivationService userActivationService;

    /**
     * Processes the action code for telephone verification.
     * It enables the associated user and consumes the code.
     *
     * @param recipient The recipient to process.
     * @param code The {@link ActionCode} to process.
     * @return An empty Optional, as enabling a user typically doesn't return a string.
     */
    @Override
    public Optional<String> process(String recipient, ActionCode code) {
        return userActivationService.activateUserAndConsumeCode(code);
    }

    /**
     * Returns the action code type handled by this processor.
     *
     * @return {@link ActionCodeType#PHONE_VERIFICATION}.
     */
    @Override
    public ActionCodeType getType() {
        return ActionCodeType.PHONE_VERIFICATION;
    }
}
