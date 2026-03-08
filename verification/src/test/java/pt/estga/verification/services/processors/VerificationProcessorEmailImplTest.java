package pt.estga.verification.services.processors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.shared.models.Email;
import pt.estga.shared.services.EmailService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationProcessorEmailImplTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationProcessorEmailImpl verificationProcessorEmail;

    private ActionCode testActionCode;

    @BeforeEach
    void setUp() {
        testActionCode = ActionCode.builder()
                .id(10L)
                .code("EMAILCODE")
                .recipient("test@example.com")
                .type(ActionCodeType.EMAIL_VERIFICATION)
                .build();
    }

    @Test
    void process_shouldSendEmailAndReturnEmptyOptional() {
        Optional<String> result = verificationProcessorEmail.process("test@example.com", testActionCode);

        assertTrue(result.isEmpty());
        verify(emailService, times(1)).sendEmail(any(Email.class));
    }

    @Test
    void process_shouldUseActionCodeRecipientFallback() {
        Optional<String> result = verificationProcessorEmail.process(null, testActionCode);

        assertTrue(result.isEmpty());
        verify(emailService, times(1)).sendEmail(any(Email.class));
    }

    @Test
    void process_shouldThrowIllegalArgumentException_whenNoRecipientAvailable() {
        testActionCode.setRecipient(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> verificationProcessorEmail.process(null, testActionCode));

        assertEquals("Recipient cannot be null for email verification.", thrown.getMessage());
        verifyNoInteractions(emailService);
    }

    @Test
    void process_shouldPropagateException_whenEmailServiceFails() {
        doThrow(new RuntimeException("Email service error")).when(emailService).sendEmail(any(Email.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> verificationProcessorEmail.process("test@example.com", testActionCode));

        assertEquals("Email service error", thrown.getMessage());
        verify(emailService, times(1)).sendEmail(any(Email.class));
    }

    @Test
    void getType_shouldReturnEmailVerification() {
        assertEquals(ActionCodeType.EMAIL_VERIFICATION, verificationProcessorEmail.getType());
    }
}
