package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.shared.exceptions.*;
import pt.estga.user.entities.User;
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
        VerificationProcessingServiceImpl serviceImpl = new VerificationProcessingServiceImpl(
                actionCodeValidationService,
                List.of(emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor),
                userVerificationActivationService
        );
        when(emailVerificationProcessor.getType()).thenReturn(ActionCodeType.EMAIL_VERIFICATION);
        when(resetPasswordProcessor.getType()).thenReturn(ActionCodeType.RESET_PASSWORD);
        when(phoneVerificationProcessor.getType()).thenReturn(ActionCodeType.PHONE_VERIFICATION);
        serviceImpl.init();
        verificationProcessingService = serviceImpl;

        clearInvocations(actionCodeValidationService,
                userVerificationActivationService, emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor);
    }

    @Test
    void confirmCode_shouldActivateContactVerification_whenCodeIsForEmailVerification() {
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
    void confirmCode_shouldActivateContactVerification_whenCodeIsForPhoneVerification() {
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
        VerificationProcessingServiceImpl serviceImpl = new VerificationProcessingServiceImpl(
                actionCodeValidationService,
                List.of(emailVerificationProcessor, phoneVerificationProcessor),
                userVerificationActivationService
        );
        when(emailVerificationProcessor.getType()).thenReturn(ActionCodeType.EMAIL_VERIFICATION);
        when(phoneVerificationProcessor.getType()).thenReturn(ActionCodeType.PHONE_VERIFICATION);
        serviceImpl.init();
        verificationProcessingService = serviceImpl;

        clearInvocations(actionCodeValidationService,
                userVerificationActivationService, emailVerificationProcessor, resetPasswordProcessor, phoneVerificationProcessor);

        String code = "codeForMissingProcessor";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.RESET_PASSWORD);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);

        assertThrows(IllegalStateException.class, () -> verificationProcessingService.confirmCode(code));
        verifyNoInteractions(userVerificationActivationService, emailVerificationProcessor, phoneVerificationProcessor);
    }

    @Test
    void processPasswordReset_shouldThrowIllegalStateException_inKeycloakOnlyMode() {
        String code = "validResetCode";
        String newPassword = "newPassword";

        assertThrows(IllegalStateException.class,
                () -> verificationProcessingService.processPasswordReset(code, newPassword));

        verifyNoInteractions(actionCodeValidationService);
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
