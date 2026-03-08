package pt.estga.verification.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.processors.VerificationProcessor;
import pt.estga.shared.exceptions.*;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;

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

    private final ActionCodeService actionCodeService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
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
     * Processes a password reset request using a valid code and a new password.
     *
     * @param code The password reset code.
     * @param newPassword The new password for the user.
     * @throws InvalidVerificationPurposeException if the code's type is not PASSWORD_RESET.
     * @throws SamePasswordException if the new password is the same as the current password.
     */
    @Transactional
    @Override
    public void processPasswordReset(String code, String newPassword) {
        log.info("Attempting to process password reset for code: {}", code);
        ActionCode actionCode = actionCodeValidationService.getValidatedActionCode(code);
        log.debug("Code {} validated for password reset. Type: {}", code, actionCode.getType());

        if (actionCode.getType() != ActionCodeType.RESET_PASSWORD) {
            log.warn("Invalid type for password reset code {}. Expected RESET_PASSWORD, got {}", code, actionCode.getType());
            throw new InvalidVerificationPurposeException(VerificationErrorMessages.INVALID_TOKEN_PURPOSE_PASSWORD_RESET);
        }

        User user = actionCode.getUser();
        log.debug("User associated with code {}: {}", code, user.getUsername());

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("Attempted to reset password for user {} with same password.", user.getUsername());
            throw new SamePasswordException(VerificationErrorMessages.SAME_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.update(user);
        log.info("Password successfully reset for user {}", user.getUsername());

        actionCodeService.consumeCode(actionCode);
        log.debug("Code {} consumed after successful password reset.", code);
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
