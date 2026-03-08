package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ActionCodeService actionCodeService;

    @InjectMocks
    private UserActivationService userActivationService;

    private User testUser;
    private ActionCode testActionCode;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .enabled(false)
                .build();

        testActionCode = ActionCode.builder()
                .id(10L)
                .code("ACTIVATIONCODE")
                .user(testUser)
                .type(ActionCodeType.TELEGRAM_VERIFICATION)
                .build();
    }

    @Test
    void activateUserAndConsumeCode_shouldEnableUserAndConsumeCode_whenUserIsDisabled() {
        Optional<String> result = userActivationService.activateUserAndConsumeCode(testActionCode);

        assertTrue(result.isEmpty());
        assertTrue(testUser.isEnabled());
        verify(userService, times(1)).update(testUser);
        verify(actionCodeService, times(1)).consumeCode(testActionCode);
    }

    @Test
    void activateUserAndConsumeCode_shouldOnlyConsumeCode_whenUserIsAlreadyEnabled() {
        testUser.setEnabled(true); // User is already enabled

        Optional<String> result = userActivationService.activateUserAndConsumeCode(testActionCode);

        assertTrue(result.isEmpty());
        assertTrue(testUser.isEnabled());
        verify(userService, never()).update(any(User.class)); // Should not update if already enabled
        verify(actionCodeService, times(1)).consumeCode(testActionCode);
    }

    @Test
    void activateUserAndConsumeCode_shouldPropagateException_whenUserServiceFails() {
        doThrow(new RuntimeException("User service error")).when(userService).update(any(User.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> userActivationService.activateUserAndConsumeCode(testActionCode));

        assertEquals("User service error", thrown.getMessage());
        assertTrue(testUser.isEnabled()); // User should still be enabled before exception
        verify(userService, times(1)).update(testUser);
        verify(actionCodeService, never()).consumeCode(any(ActionCode.class)); // Should not consume if update fails
    }

    @Test
    void activateUserAndConsumeCode_shouldPropagateException_whenActionCodeServiceFails() {
        doThrow(new RuntimeException("Action code service error")).when(actionCodeService).consumeCode(any(ActionCode.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> userActivationService.activateUserAndConsumeCode(testActionCode));

        assertEquals("Action code service error", thrown.getMessage());
        assertTrue(testUser.isEnabled()); // User should still be enabled
        verify(userService, times(1)).update(testUser);
        verify(actionCodeService, times(1)).consumeCode(testActionCode);
    }
}
