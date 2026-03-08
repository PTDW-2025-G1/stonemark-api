package pt.estga.verification.services.processors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.estga.shared.models.Email;
import pt.estga.shared.services.EmailService;
import pt.estga.user.entities.User;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationProcessorPasswordResetImplTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationProcessorPasswordResetImpl verificationProcessorPasswordReset;

    private ActionCode testActionCode;
    private final String TEST_CODE_STRING = "RESETCODE123";

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .id(1L)
                .username("testuser")
                .firstName("Test")
                .email("test@example.com")
                .build();

        testActionCode = ActionCode.builder()
                .id(10L)
                .code(TEST_CODE_STRING)
                .recipient("test@example.com")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(1800))
                .type(ActionCodeType.RESET_PASSWORD)
                .build();

        ReflectionTestUtils.setField(verificationProcessorPasswordReset, "resetPasswordUrl", "http://localhost:3000/reset-password");
    }

    @Test
    void process_shouldSendEmailAndReturnOptionalContainingCodeString() {
        Optional<String> result = verificationProcessorPasswordReset.process("test@example.com", testActionCode);

        assertTrue(result.isPresent());
        assertEquals(TEST_CODE_STRING, result.get());
        verify(emailService, times(1)).sendEmail(any(Email.class));
    }

    @Test
    void process_shouldUseActionCodeRecipientFallback() {
        Optional<String> result = verificationProcessorPasswordReset.process(null, testActionCode);

        assertTrue(result.isPresent());
        assertEquals(TEST_CODE_STRING, result.get());
        verify(emailService, times(1)).sendEmail(any(Email.class));
    }

    @Test
    void process_shouldReturnCodeWithoutSendingEmail_whenNoRecipientAvailable() {
        testActionCode.setRecipient(null);
        testActionCode.getUser().setEmail(null);

        Optional<String> result = verificationProcessorPasswordReset.process(null, testActionCode);

        assertTrue(result.isPresent());
        assertEquals(TEST_CODE_STRING, result.get());
        verifyNoInteractions(emailService);
    }

    @Test
    void getType_shouldReturnResetPassword() {
        assertEquals(ActionCodeType.RESET_PASSWORD, verificationProcessorPasswordReset.getType());
    }
}
