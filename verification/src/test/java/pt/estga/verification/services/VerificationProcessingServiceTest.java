package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.estga.shared.exceptions.*;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.processors.VerificationProcessor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationProcessingServiceTest {

    @Mock
    private ActionCodeService actionCodeService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActionCodeValidationService actionCodeValidationService;

    @Mock
    private UserVerificationActivationService userVerificationActivationService;

    @Mock
    private VerificationProcessor emailVerificationProcessor;

    @Mock
    private VerificationProcessor resetPasswordProcessor;

    @Mock
    private VerificationProcessor phoneVerificationProcessor;

    private VerificationProcessingService verificationProcessingService;

    @BeforeEach
    void setUp() {
        // Initialize with all known processors
        VerificationProcessingServiceImpl serviceImpl = new VerificationProcessingServiceImpl(
                actionCodeService,
                userService,
                passwordEncoder,
                actionCodeValidationService,
                List.of(emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor),
                userVerificationActivationService
        );
        when(emailVerificationProcessor.getType()).thenReturn(ActionCodeType.EMAIL_VERIFICATION);
        when(resetPasswordProcessor.getType()).thenReturn(ActionCodeType.RESET_PASSWORD);
        when(phoneVerificationProcessor.getType()).thenReturn(ActionCodeType.PHONE_VERIFICATION);
        serviceImpl.init();
        verificationProcessingService = serviceImpl;

        clearInvocations(actionCodeService, userService, passwordEncoder, actionCodeValidationService,
                userVerificationActivationService, emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor);
    }

    @Test
    void confirmCode_shouldActivateUserContact_whenCodeIsForEmailVerification() {
        String code = "validEmailCode";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.EMAIL_VERIFICATION);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);
        when(userVerificationActivationService.activateContactVerification(actionCode)).thenReturn(Optional.empty());

        Optional<String> result = verificationProcessingService.confirmCode(code);

        assertTrue(result.isEmpty());
        verify(userVerificationActivationService).activateContactVerification(actionCode);
        verifyNoInteractions(emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor);
    }

    @Test
    void confirmCode_shouldActivateUserContact_whenCodeIsForPhoneVerification() {
        String code = "validPhoneCode";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.PHONE_VERIFICATION);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);
        when(userVerificationActivationService.activateContactVerification(actionCode)).thenReturn(Optional.of("someValue"));

        Optional<String> result = verificationProcessingService.confirmCode(code);

        assertTrue(result.isPresent());
        assertEquals("someValue", result.get());
        verify(userVerificationActivationService).activateContactVerification(actionCode);
        verifyNoInteractions(emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor);
    }

    @Test
    void confirmCode_shouldProcessPasswordReset_whenCodeIsValid() {
        String code = "validResetCode";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.RESET_PASSWORD);
        actionCode.setRecipient("user@example.com");

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);
        when(resetPasswordProcessor.process(eq("user@example.com"), eq(actionCode))).thenReturn(Optional.of(code));

        Optional<String> result = verificationProcessingService.confirmCode(code);

        assertTrue(result.isPresent());
        assertEquals(code, result.get());
        verify(resetPasswordProcessor).process(eq("user@example.com"), eq(actionCode));
        verifyNoInteractions(userVerificationActivationService, emailVerificationProcessor, phoneVerificationProcessor);
    }

    @Test
    void confirmCode_shouldThrowInvalidVerificationPurposeException_whenCodeTypeIsInvalid() {
        String code = "invalidTypeCode";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.DEVICE_VERIFICATION);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);

        assertThrows(InvalidVerificationPurposeException.class, () -> verificationProcessingService.confirmCode(code));
        verifyNoInteractions(userVerificationActivationService, emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor);
    }

    @Test
    void confirmCode_shouldThrowIllegalStateException_whenProcessorIsMissingForValidType() {
        // Re-initialize service with a missing processor for RESET_PASSWORD
        VerificationProcessingServiceImpl serviceImpl = new VerificationProcessingServiceImpl(
                actionCodeService,
                userService,
                passwordEncoder,
                actionCodeValidationService,
                List.of(emailVerificationProcessor, phoneVerificationProcessor),
                userVerificationActivationService
        );
        when(emailVerificationProcessor.getType()).thenReturn(ActionCodeType.EMAIL_VERIFICATION);
        when(phoneVerificationProcessor.getType()).thenReturn(ActionCodeType.PHONE_VERIFICATION);
        serviceImpl.init();
        verificationProcessingService = serviceImpl;

        // Clear invocations for this specific setup as well
        clearInvocations(actionCodeService, userService, passwordEncoder, actionCodeValidationService,
                userVerificationActivationService, emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor);


        String code = "codeForMissingProcessor";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.RESET_PASSWORD); // This type is valid for confirmation but its processor is missing

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);

        assertThrows(IllegalStateException.class, () -> verificationProcessingService.confirmCode(code));
        verifyNoInteractions(userVerificationActivationService, emailVerificationProcessor, phoneVerificationProcessor);
    }

    @Test
    void processPasswordReset_shouldResetPassword_whenCodeIsValid() {
        String code = "validResetCode";
        String newPassword = "newPassword";
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("oldEncodedPassword");
        ActionCode actionCode = new ActionCode();
        actionCode.setUser(user);
        actionCode.setType(ActionCodeType.RESET_PASSWORD);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);
        when(passwordEncoder.matches(newPassword, "oldEncodedPassword")).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");

        verificationProcessingService.processPasswordReset(code, newPassword);

        verify(userService).update(user);
        assertEquals("newEncodedPassword", user.getPassword());
        verify(actionCodeService).consumeCode(actionCode);
    }

    @Test
    void processPasswordReset_shouldThrowInvalidVerificationPurposeException_whenCodeTypeIsInvalid() {
        String code = "invalidTypeCode";
        String newPassword = "newPassword";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.EMAIL_VERIFICATION);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);

        assertThrows(InvalidVerificationPurposeException.class,
                () -> verificationProcessingService.processPasswordReset(code, newPassword));
        verifyNoInteractions(userService, passwordEncoder, actionCodeService);
    }

    @Test
    void processPasswordReset_shouldThrowSamePasswordException_whenPasswordIsTheSame() {
        String code = "validResetCode";
        String newPassword = "samePassword";
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("encodedSamePassword");
        ActionCode actionCode = new ActionCode();
        actionCode.setUser(user);
        actionCode.setType(ActionCodeType.RESET_PASSWORD);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);
        when(passwordEncoder.matches(newPassword, "encodedSamePassword")).thenReturn(true);

        assertThrows(SamePasswordException.class,
                () -> verificationProcessingService.processPasswordReset(code, newPassword));
        verifyNoInteractions(userService, actionCodeService);
    }

    @Test
    void validatePasswordResetToken_shouldReturnUser_whenTokenIsValidAndTypeIsResetPassword() {
        String code = "validResetToken";
        User user = new User();
        user.setUsername("testUser");
        ActionCode actionCode = new ActionCode();
        actionCode.setUser(user);
        actionCode.setType(ActionCodeType.RESET_PASSWORD);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);

        Optional<User> result = verificationProcessingService.validatePasswordResetToken(code);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void validatePasswordResetToken_shouldReturnEmpty_whenTokenIsValidButTypeIsNotResetPassword() {
        String code = "validEmailToken";
        User user = new User();
        user.setUsername("testUser");
        ActionCode actionCode = new ActionCode();
        actionCode.setUser(user);
        actionCode.setType(ActionCodeType.EMAIL_VERIFICATION);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);

        Optional<User> result = verificationProcessingService.validatePasswordResetToken(code);

        assertTrue(result.isEmpty());
    }

    @Test
    void validatePasswordResetToken_shouldReturnEmpty_whenTokenIsInvalid() {
        String code = "invalidCode";

        when(actionCodeValidationService.getValidatedActionCode(code)).thenThrow(new InvalidActionCodeException("Invalid code"));

        Optional<User> result = verificationProcessingService.validatePasswordResetToken(code);

        assertTrue(result.isEmpty());
    }

    @Test
    void validatePasswordResetToken_shouldReturnEmpty_whenTokenIsExpired() {
        String code = "expiredCode";

        when(actionCodeValidationService.getValidatedActionCode(code)).thenThrow(new ActionCodeExpiredException("Expired code"));

        Optional<User> result = verificationProcessingService.validatePasswordResetToken(code);

        assertTrue(result.isEmpty());
    }

    @Test
    void validatePasswordResetToken_shouldReturnEmpty_whenTokenIsConsumed() {
        String code = "consumedCode";

        when(actionCodeValidationService.getValidatedActionCode(code)).thenThrow(new ActionCodeConsumedException("Consumed code"));

        Optional<User> result = verificationProcessingService.validatePasswordResetToken(code);

        assertTrue(result.isEmpty());
    }
}
