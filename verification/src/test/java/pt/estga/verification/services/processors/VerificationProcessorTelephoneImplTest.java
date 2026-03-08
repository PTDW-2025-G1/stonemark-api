package pt.estga.verification.services.processors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.user.entities.User;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.UserActivationService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationProcessorTelephoneImplTest {

    @Mock
    private UserActivationService userActivationService;

    @InjectMocks
    private VerificationProcessorTelephoneImpl verificationProcessorTelephone;

    private ActionCode testActionCode;

    @BeforeEach
    void setUp() {
        User testUser = User.builder().id(1L).username("testuser").build();
        testActionCode = ActionCode.builder()
                .id(10L)
                .code("PHONECODE")
                .recipient("123456789")
                .user(testUser)
                .type(ActionCodeType.PHONE_VERIFICATION)
                .build();
    }

    @Test
    void process_shouldDelegateToUserActivationService() {
        when(userActivationService.activateUserAndConsumeCode(testActionCode)).thenReturn(Optional.empty());

        Optional<String> result = verificationProcessorTelephone.process("123456789", testActionCode);

        assertTrue(result.isEmpty());
        verify(userActivationService, times(1)).activateUserAndConsumeCode(testActionCode);
    }

    @Test
    void process_shouldPropagateException_whenUserActivationServiceFails() {
        doThrow(new RuntimeException("Activation service error")).when(userActivationService).activateUserAndConsumeCode(any(ActionCode.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> verificationProcessorTelephone.process("123456789", testActionCode));

        assertEquals("Activation service error", thrown.getMessage());
        verify(userActivationService, times(1)).activateUserAndConsumeCode(testActionCode);
    }

    @Test
    void getType_shouldReturnPhoneVerification() {
        assertEquals(ActionCodeType.PHONE_VERIFICATION, verificationProcessorTelephone.getType());
    }
}
