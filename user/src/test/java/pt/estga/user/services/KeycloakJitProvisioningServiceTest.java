package pt.estga.user.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.dtos.KeycloakIdentitySnapshot;
import pt.estga.user.entities.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakJitProvisioningServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private KeycloakJitProvisioningServiceImpl jitProvisioningService;

    private User existingUser;
    private KeycloakIdentitySnapshot newUserSnapshot;
    private KeycloakIdentitySnapshot existingUserSnapshot;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .username("existing-user")
                .email("existing@example.com")
                .emailVerified(true)
                .phone("+351912345678")
                .phoneVerified(true)
                .keycloakSub("existing-sub-123")
                .enabled(true)
                .role(UserRole.USER)
                .build();

        newUserSnapshot = new KeycloakIdentitySnapshot(
                "new-sub-456",
                "newuser",
                "New",
                "User",
                "new@example.com",
                true,
                "+351987654321",
                true
        );

        existingUserSnapshot = new KeycloakIdentitySnapshot(
                "existing-sub-123",
                "existing-user",
                "Existing",
                "User",
                "existing@example.com",
                true,
                "+351912345678",
                true
        );
    }

    @Test
    void resolveOrProvision_shouldReturnExistingUser_whenKeycloakSubExists() {
        when(userService.findByKeycloakSub("existing-sub-123")).thenReturn(Optional.of(existingUser));
        when(userService.update(any(User.class))).thenReturn(existingUser);

        User result = jitProvisioningService.resolveOrProvision(existingUserSnapshot);

        assertNotNull(result);
        assertEquals("existing-sub-123", result.getKeycloakSub());
        assertEquals("existing@example.com", result.getEmail());
        verify(userService).findByKeycloakSub("existing-sub-123");
        verify(userService).update(existingUser);
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void resolveOrProvision_shouldCreateNewUser_whenKeycloakSubNotFoundAndNoEmailMatch() {
        when(userService.findByKeycloakSub("new-sub-456")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.existsByUsername(anyString())).thenReturn(false);
        when(userService.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = jitProvisioningService.resolveOrProvision(newUserSnapshot);

        assertNotNull(result);
        assertEquals("new-sub-456", result.getKeycloakSub());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("newuser", result.getUsername());
        assertEquals("New", result.getFirstName());
        assertEquals("User", result.getLastName());
        assertTrue(result.isEmailVerified());
        assertTrue(result.isPhoneVerified());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.isEnabled());

        verify(userService).findByKeycloakSub("new-sub-456");
        verify(userService).findByEmail("new@example.com");
        verify(userService).create(any(User.class));
    }

    @Test
    void resolveOrProvision_shouldLinkExistingUserByEmail_whenVerified() {
        User userWithoutKeycloak = User.builder()
                .id(3L)
                .username("legacy-user")
                .email("new@example.com")
                .emailVerified(true)
                .keycloakSub(null)
                .enabled(true)
                .role(UserRole.USER)
                .build();

        when(userService.findByKeycloakSub("new-sub-456")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.of(userWithoutKeycloak));
        when(userService.update(any(User.class))).thenReturn(userWithoutKeycloak);

        User result = jitProvisioningService.resolveOrProvision(newUserSnapshot);

        assertNotNull(result);
        assertEquals("new-sub-456", result.getKeycloakSub());
        assertEquals("new@example.com", result.getEmail());

        verify(userService).findByKeycloakSub("new-sub-456");
        verify(userService).findByEmail("new@example.com");
        verify(userService).update(userWithoutKeycloak);
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void resolveOrProvision_shouldThrowException_whenEmailAlreadyLinkedToDifferentKeycloakSub() {
        User userWithDifferentSub = User.builder()
                .id(4L)
                .username("other-user")
                .email("new@example.com")
                .emailVerified(true)
                .keycloakSub("other-sub-789")
                .enabled(true)
                .role(UserRole.USER)
                .build();

        when(userService.findByKeycloakSub("new-sub-456")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.of(userWithDifferentSub));

        assertThrows(IllegalStateException.class, () -> jitProvisioningService.resolveOrProvision(newUserSnapshot));

        verify(userService).findByKeycloakSub("new-sub-456");
        verify(userService).findByEmail("new@example.com");
        verify(userService, never()).update(any(User.class));
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void resolveOrProvision_shouldCreateUser_whenEmailUnverified() {
        KeycloakIdentitySnapshot unverifiedSnapshot = new KeycloakIdentitySnapshot(
                "unverified-sub",
                "unverified-user",
                "Unverified",
                "User",
                "unverified@example.com",
                false,
                null,
                false
        );

        User newUser = User.builder()
                .id(5L)
                .username("unverified-user")
                .email("unverified@example.com")
                .emailVerified(false)
                .keycloakSub("unverified-sub")
                .enabled(true)
                .role(UserRole.USER)
                .build();

        when(userService.findByKeycloakSub("unverified-sub")).thenReturn(Optional.empty());
        when(userService.existsByUsername(anyString())).thenReturn(false);
        when(userService.create(any(User.class))).thenReturn(newUser);

        User result = jitProvisioningService.resolveOrProvision(unverifiedSnapshot);

        assertNotNull(result);
        assertEquals("unverified-sub", result.getKeycloakSub());
        assertFalse(result.isEmailVerified());

        verify(userService).findByKeycloakSub("unverified-sub");
        verify(userService, never()).findByEmail(anyString());
        verify(userService).create(any(User.class));
    }

    @Test
    void resolveOrProvision_shouldSyncSnapshotFields_whenUserExists() {
        KeycloakIdentitySnapshot updatedSnapshot = new KeycloakIdentitySnapshot(
                "existing-sub-123",
                "updated-username",
                "Updated",
                "Name",
                "updated@example.com",
                true,
                "+351999888777",
                true
        );

        when(userService.findByKeycloakSub("existing-sub-123")).thenReturn(Optional.of(existingUser));
        when(userService.update(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = jitProvisioningService.resolveOrProvision(updatedSnapshot);

        assertNotNull(result);
        assertEquals("updated-username", result.getUsername());
        assertEquals("Updated", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("+351999888777", result.getPhone());
        assertTrue(result.isEmailVerified());
        assertTrue(result.isPhoneVerified());

        verify(userService).update(existingUser);
    }

    @Test
    void resolveOrProvision_shouldHandleMissingPhone_gracefully() {
        KeycloakIdentitySnapshot noPhoneSnapshot = new KeycloakIdentitySnapshot(
                "no-phone-sub",
                "nophone",
                "No",
                "Phone",
                "nophone@example.com",
                true,
                null,
                false
        );

        User newUser = User.builder()
                .id(6L)
                .username("nophone")
                .email("nophone@example.com")
                .emailVerified(true)
                .phone(null)
                .phoneVerified(false)
                .keycloakSub("no-phone-sub")
                .enabled(true)
                .role(UserRole.USER)
                .build();

        when(userService.findByKeycloakSub("no-phone-sub")).thenReturn(Optional.empty());
        when(userService.findByEmail("nophone@example.com")).thenReturn(Optional.empty());
        when(userService.existsByUsername(anyString())).thenReturn(false);
        when(userService.create(any(User.class))).thenReturn(newUser);

        User result = jitProvisioningService.resolveOrProvision(noPhoneSnapshot);

        assertNotNull(result);
        assertNull(result.getPhone());
        assertFalse(result.isPhoneVerified());
        assertTrue(result.isEmailVerified());

        verify(userService).create(any(User.class));
    }

    @Test
    void resolveOrProvision_shouldGenerateUniqueUsername_whenPreferredUsernameExists() {
        KeycloakIdentitySnapshot duplicateUsernameSnapshot = new KeycloakIdentitySnapshot(
                "duplicate-sub",
                "existing-user",
                "Dup",
                "User",
                "duplicate@example.com",
                true,
                null,
                false
        );

        User newUser = User.builder()
                .id(7L)
                .username("existing-user1")
                .email("duplicate@example.com")
                .emailVerified(true)
                .keycloakSub("duplicate-sub")
                .enabled(true)
                .role(UserRole.USER)
                .build();

        when(userService.findByKeycloakSub("duplicate-sub")).thenReturn(Optional.empty());
        when(userService.findByEmail("duplicate@example.com")).thenReturn(Optional.empty());
        when(userService.existsByUsername("existing-user")).thenReturn(true);
        when(userService.existsByUsername("existing-user1")).thenReturn(false);
        when(userService.create(any(User.class))).thenReturn(newUser);

        User result = jitProvisioningService.resolveOrProvision(duplicateUsernameSnapshot);

        assertNotNull(result);
        assertEquals("existing-user1", result.getUsername());

        verify(userService).existsByUsername("existing-user");
        verify(userService).existsByUsername("existing-user1");
        verify(userService).create(any(User.class));
    }
}
