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
    private VerificationProcessor telegramVerificationProcessor;

    @InjectMocks
    private ActionCodeDispatchServiceImpl actionCodeDispatchService;

    private String recipient;

    @BeforeEach
    void setUp() {
        recipient = "123456789";

        when(telegramVerificationProcessor.getType()).thenReturn(ActionCodeType.TELEGRAM_VERIFICATION);
        when(telegramVerificationProcessor.process(any(), any())).thenReturn(Optional.empty());

        actionCodeDispatchService = new ActionCodeDispatchServiceImpl(List.of(telegramVerificationProcessor));
        actionCodeDispatchService.init();

        clearInvocations(telegramVerificationProcessor);
    }

    @Test
    void sendVerification_shouldDispatchToCorrectProcessor_forTelegramVerification() {
        ActionCode telegramCode = ActionCode.builder().type(ActionCodeType.TELEGRAM_VERIFICATION).build();

        actionCodeDispatchService.sendVerification(recipient, telegramCode);

        verify(telegramVerificationProcessor, times(1)).process(recipient, telegramCode);
    }

    @Test
    void sendVerification_shouldNotDispatch_whenNoProcessorFound() {
        ActionCode unknownTypeCode = ActionCode.builder().type(ActionCodeType.DEVICE_VERIFICATION).build();

        actionCodeDispatchService.sendVerification(recipient, unknownTypeCode);

        verifyNoInteractions(telegramVerificationProcessor);
    }

    @Test
    void sendVerification_shouldPropagateException_whenProcessorThrowsException() {
        ActionCode telegramCode = ActionCode.builder().type(ActionCodeType.TELEGRAM_VERIFICATION).build();
        doThrow(new RuntimeException("Processor failed")).when(telegramVerificationProcessor).process(recipient, telegramCode);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> actionCodeDispatchService.sendVerification(recipient, telegramCode));

        assertEquals("Processor failed", thrown.getMessage());
        verify(telegramVerificationProcessor, times(1)).process(recipient, telegramCode);
    }
}
