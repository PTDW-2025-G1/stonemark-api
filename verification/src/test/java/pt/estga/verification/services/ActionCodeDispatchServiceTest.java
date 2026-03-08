package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.processors.VerificationProcessor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionCodeDispatchServiceTest {

    @Mock
    private VerificationProcessor emailVerificationProcessor;

    @Mock
    private VerificationProcessor phoneVerificationProcessor;

    @InjectMocks
    private ActionCodeDispatchServiceImpl actionCodeDispatchService;

    private String recipient;

    @BeforeEach
    void setUp() {
        recipient = "test@example.com";

        when(emailVerificationProcessor.getType()).thenReturn(ActionCodeType.EMAIL_VERIFICATION);
        when(phoneVerificationProcessor.getType()).thenReturn(ActionCodeType.PHONE_VERIFICATION);
        when(emailVerificationProcessor.process(any(), any())).thenReturn(Optional.empty());
        when(phoneVerificationProcessor.process(any(), any())).thenReturn(Optional.empty());

        actionCodeDispatchService = new ActionCodeDispatchServiceImpl(List.of(emailVerificationProcessor, phoneVerificationProcessor));
        actionCodeDispatchService.init();

        clearInvocations(emailVerificationProcessor, phoneVerificationProcessor);
    }

    @Test
    void sendVerification_shouldDispatchToCorrectProcessor_forEmailVerification() {
        ActionCode emailCode = ActionCode.builder().type(ActionCodeType.EMAIL_VERIFICATION).build();

        actionCodeDispatchService.sendVerification(recipient, emailCode);

        verify(emailVerificationProcessor, times(1)).process(recipient, emailCode);
        verifyNoInteractions(phoneVerificationProcessor);
    }

    @Test
    void sendVerification_shouldDispatchToCorrectProcessor_forPhoneVerification() {
        ActionCode phoneCode = ActionCode.builder().type(ActionCodeType.PHONE_VERIFICATION).build();

        actionCodeDispatchService.sendVerification(recipient, phoneCode);

        verify(phoneVerificationProcessor, times(1)).process(recipient, phoneCode);
        verifyNoInteractions(emailVerificationProcessor);
    }

    @Test
    void sendVerification_shouldNotDispatch_whenNoProcessorFound() {
        ActionCode unknownTypeCode = ActionCode.builder().type(ActionCodeType.DEVICE_VERIFICATION).build();

        actionCodeDispatchService.sendVerification(recipient, unknownTypeCode);

        verifyNoInteractions(emailVerificationProcessor, phoneVerificationProcessor);
    }

    @Test
    void sendVerification_shouldPropagateException_whenProcessorThrowsException() {
        ActionCode emailCode = ActionCode.builder().type(ActionCodeType.EMAIL_VERIFICATION).build();
        doThrow(new RuntimeException("Processor failed")).when(emailVerificationProcessor).process(recipient, emailCode);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> actionCodeDispatchService.sendVerification(recipient, emailCode));

        assertEquals("Processor failed", thrown.getMessage());
        verify(emailVerificationProcessor, times(1)).process(recipient, emailCode);
        verifyNoInteractions(phoneVerificationProcessor);
    }
}
