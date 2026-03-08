package pt.estga.verification.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.exceptions.*;
import pt.estga.user.entities.User;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.processors.VerificationProcessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for processing action codes,
 * including confirmation and password reset operations.
 * It uses a strategy pattern to handle different action code types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationProcessingServiceImpl implements VerificationProcessingService {

    private final ActionCodeValidationService actionCodeValidationService;
    private final List<VerificationProcessor> purposeProcessors;
    private final UserVerificationActivationService userVerificationActivationService;

    private Map<ActionCodeType, VerificationProcessor> processorMap;

    // Define types that are valid for code confirmation
    private static final Set<ActionCodeType> VALID_CODE_CONFIRMATION_TYPES = Set.of(
            ActionCodeType.EMAIL_VERIFICATION,
            ActionCodeType.PHONE_VERIFICATION,
            ActionCodeType.RESET_PASSWORD
    );

    /**
     * Initializes the processor map after all dependencies are injected.
     * This maps each {@link ActionCodeType} to its corresponding {@link VerificationProcessor}.
     */
    @PostConstruct
    public void init() {
        processorMap = purposeProcessors.stream()
                .collect(Collectors.toMap(VerificationProcessor::getType, Function.identity()));
    }

    /**
     * Confirms an action code. The action taken depends on the code's type.
     *
     * @param code The action code string.
     * @return An Optional string, which might be the code itself for password reset, or empty otherwise.
     * @throws InvalidVerificationPurposeException if the code's type is not supported for confirmation.
     */
    @Transactional
    @Override
    public Optional<String> confirmCode(String code) {
        log.info("Attempting to confirm code: {}", code);
        ActionCode actionCode = actionCodeValidationService.getValidatedActionCode(code);
        log.debug("Code {} validated successfully. Type: {}", code, actionCode.getType());

        ActionCodeType type = actionCode.getType();

        if (!VALID_CODE_CONFIRMATION_TYPES.contains(type)) {
            log.warn("Invalid type for code confirmation: {}", type);
            throw new InvalidVerificationPurposeException("Invalid type for code confirmation: " + type);
        }

        // For email and phone verification, the action is to activate the user contact.
        if (type == ActionCodeType.EMAIL_VERIFICATION || type == ActionCodeType.PHONE_VERIFICATION) {
            log.debug("Processing user verification activation for code type: {}", type);
            return userVerificationActivationService.activateContactVerification(actionCode);
        }

        // For other types (like password reset), use the specific processor.
        VerificationProcessor processor = processorMap.get(type);
        if (processor == null) {
            log.error("Internal configuration error: No processor registered for action code type: {}", type);
            throw new IllegalStateException("Internal configuration error: No processor registered for action code type: " + type);
        }
        log.debug("Using processor {} for code type: {}", processor.getClass().getSimpleName(), type);
        return processor.process(actionCode.getRecipient(), actionCode);
    }

    /**
     * Password reset is no longer handled locally. Authentication credentials are managed by Keycloak.
     */
    @Transactional
    @Override
    public void processPasswordReset(String code, String newPassword) {
        log.warn("Password reset requested for code {}, but local password management is disabled in Keycloak-only mode", code);
        throw new IllegalStateException("Password reset is handled by Keycloak and is no longer available in this API.");
    }

    /**
     * Validates a password reset code without processing the reset itself.
     *
     * @param code The password reset code string.
     * @return An Optional containing the User if the code is valid and for password reset, otherwise empty.
     */
    @Override
    public Optional<User> validatePasswordResetToken(String code) {
        try {
            ActionCode actionCode = actionCodeValidationService.getValidatedActionCode(code);
            if (actionCode.getType() == ActionCodeType.RESET_PASSWORD) {
                return Optional.of(actionCode.getUser());
            }
        } catch (InvalidActionCodeException | ActionCodeExpiredException | ActionCodeConsumedException e) {
            log.debug("Validation failed for password reset token: {}", code, e);
        }
        return Optional.empty();
    }
}
