package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationDispatchServiceTest {

    @Mock
    private ActionCodeDispatchService actionCodeDispatchService;

    @InjectMocks
    private VerificationDispatchServiceImpl verificationDispatchService;

    private ActionCode testActionCode;
    private String recipient;

    @BeforeEach
    void setUp() {
        recipient = "123456789";
        testActionCode = ActionCode.builder()
                .id(10L)
                .code("CODE123")
                .type(ActionCodeType.TELEGRAM_VERIFICATION)
                .recipient(recipient)
                .build();
    }

    @Test
    void sendVerification_shouldDelegateToActionCodeDispatchService() {
        verificationDispatchService.sendVerification(recipient, testActionCode);

        verify(actionCodeDispatchService, times(1))
                .sendVerification(recipient, testActionCode);
    }

    @Test
    void sendVerification_shouldPropagateException_whenActionCodeDispatchServiceThrowsException() {
        doThrow(new RuntimeException("Dispatch failed"))
                .when(actionCodeDispatchService)
                .sendVerification(anyString(), any(ActionCode.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> verificationDispatchService.sendVerification(recipient, testActionCode));

        assertEquals("Dispatch failed", thrown.getMessage());
        verify(actionCodeDispatchService, times(1))
                .sendVerification(recipient, testActionCode);
    }
}
