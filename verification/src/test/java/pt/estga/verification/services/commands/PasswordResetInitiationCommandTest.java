package pt.estga.verification.services.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.shared.exceptions.ContactMethodNotAvailableException;
import pt.estga.shared.exceptions.UserNotFoundException;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.services.ActionCodeService;
import pt.estga.verification.services.VerificationDispatchService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetInitiationCommandTest {

    @Mock
    private UserService userService;

    @Mock
    private ActionCodeService actionCodeService;

    @Mock
    private VerificationDispatchService verificationDispatchService;

    @InjectMocks
    private PasswordResetInitiationCommand passwordResetInitiationCommand;

    private User testUser;
    private ActionCode testActionCode;
    private final String CONTACT_VALUE = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email(CONTACT_VALUE)
                .emailVerified(true)
                .enabled(true)
                .build();

        testActionCode = ActionCode.builder()
                .id(100L)
                .code("RESETCODE")
                .recipient(CONTACT_VALUE)
                .user(testUser)
                .type(ActionCodeType.RESET_PASSWORD)
                .build();
    }

    @Test
    void execute_shouldReturnRunnableThatDispatchesVerification_whenContactIsValid() {
        when(userService.findByEmail(CONTACT_VALUE)).thenReturn(Optional.of(testUser));
        when(actionCodeService.createAndSave(testUser, CONTACT_VALUE, ActionCodeType.RESET_PASSWORD)).thenReturn(testActionCode);

        Runnable runnable = passwordResetInitiationCommand.execute(CONTACT_VALUE);
        assertNotNull(runnable);

        // Execute the runnable to trigger the dispatch
        runnable.run();

        verify(userService, times(1)).findByEmail(CONTACT_VALUE);
        verify(actionCodeService, times(1)).createAndSave(testUser, CONTACT_VALUE, ActionCodeType.RESET_PASSWORD);
        verify(verificationDispatchService, times(1)).sendVerification(CONTACT_VALUE, testActionCode);
    }

    @Test
    void execute_shouldThrowUserNotFoundException_whenContactNotFound() {
        when(userService.findByEmail(CONTACT_VALUE)).thenReturn(Optional.empty());
        when(userService.findByPhone(CONTACT_VALUE)).thenReturn(Optional.empty());

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class,
                () -> passwordResetInitiationCommand.execute(CONTACT_VALUE));

        assertEquals("User not found with contact: " + CONTACT_VALUE, thrown.getMessage());
        verify(userService, times(1)).findByEmail(CONTACT_VALUE);
        verify(userService, times(1)).findByPhone(CONTACT_VALUE);
        verifyNoInteractions(actionCodeService, verificationDispatchService);
    }

    @Test
    void execute_shouldThrowUserNotFoundException_whenUserIsNotEnabled() {
        testUser.setEnabled(false);
        when(userService.findByEmail(CONTACT_VALUE)).thenReturn(Optional.of(testUser));

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class,
                () -> passwordResetInitiationCommand.execute(CONTACT_VALUE));

        assertEquals("User not found with contact: " + CONTACT_VALUE, thrown.getMessage());
        verifyNoInteractions(actionCodeService, verificationDispatchService);
    }

    @Test
    void execute_shouldThrowContactMethodNotAvailableException_whenEmailIsNotVerified() {
        testUser.setEmailVerified(false);
        when(userService.findByEmail(CONTACT_VALUE)).thenReturn(Optional.of(testUser));

        ContactMethodNotAvailableException thrown = assertThrows(ContactMethodNotAvailableException.class,
                () -> passwordResetInitiationCommand.execute(CONTACT_VALUE));

        assertEquals("Contact is not verified: " + CONTACT_VALUE, thrown.getMessage());
        verifyNoInteractions(actionCodeService, verificationDispatchService);
    }

    @Test
    void execute_shouldPropagateException_whenActionCodeServiceFails() {
        when(userService.findByEmail(CONTACT_VALUE)).thenReturn(Optional.of(testUser));
        when(actionCodeService.createAndSave(testUser, CONTACT_VALUE, ActionCodeType.RESET_PASSWORD))
                .thenThrow(new RuntimeException("ActionCodeService error"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> passwordResetInitiationCommand.execute(CONTACT_VALUE));

        assertEquals("ActionCodeService error", thrown.getMessage());
        verify(actionCodeService, times(1)).createAndSave(testUser, CONTACT_VALUE, ActionCodeType.RESET_PASSWORD);
        verifyNoInteractions(verificationDispatchService);
    }

    @Test
    void execute_shouldPropagateException_whenDispatchServiceFailsDuringRunnableExecution() {
        when(userService.findByEmail(CONTACT_VALUE)).thenReturn(Optional.of(testUser));
        when(actionCodeService.createAndSave(testUser, CONTACT_VALUE, ActionCodeType.RESET_PASSWORD)).thenReturn(testActionCode);
        doThrow(new RuntimeException("Dispatch error")).when(verificationDispatchService)
                .sendVerification(CONTACT_VALUE, testActionCode);

        Runnable runnable = passwordResetInitiationCommand.execute(CONTACT_VALUE);
        assertNotNull(runnable);

        RuntimeException thrown = assertThrows(RuntimeException.class, runnable::run);

        assertEquals("Dispatch error", thrown.getMessage());
        verify(verificationDispatchService, times(1)).sendVerification(CONTACT_VALUE, testActionCode);
    }
}
