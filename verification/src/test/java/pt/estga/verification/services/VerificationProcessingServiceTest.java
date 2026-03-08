package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.shared.exceptions.*;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.processors.VerificationProcessor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private VerificationProcessor phoneVerificationProcessor;

    private VerificationProcessingService verificationProcessingService;

    @BeforeEach
    void setUp() {
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
                userVerificationActivationService, emailVerificationProcessor, phoneVerificationProcessor);
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
        verifyNoInteractions(emailVerificationProcessor, phoneVerificationProcessor);
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
        verifyNoInteractions(emailVerificationProcessor, phoneVerificationProcessor);
    }

    @Test
    void confirmCode_shouldThrowInvalidVerificationPurposeException_whenCodeTypeIsInvalid() {
        String code = "invalidTypeCode";
        ActionCode actionCode = new ActionCode();
        actionCode.setType(ActionCodeType.DEVICE_VERIFICATION);

        when(actionCodeValidationService.getValidatedActionCode(code)).thenReturn(actionCode);

        assertThrows(InvalidVerificationPurposeException.class, () -> verificationProcessingService.confirmCode(code));
        verifyNoInteractions(userVerificationActivationService, emailVerificationProcessor, phoneVerificationProcessor);
    }
}

