package pt.estga.user.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.user.dtos.KeycloakIdentitySnapshot;
import pt.estga.user.entities.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakJitProvisioningServiceTest {

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private KeycloakJitProvisioningService service;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
    }

    @Test
    void resolveOrProvision_rejectsMissingSub() {
        KeycloakIdentitySnapshot snapshot = new KeycloakIdentitySnapshot(null, "u", null, null, null, false);

        assertThrows(IllegalArgumentException.class, () -> service.resolveOrProvision(snapshot));
    }

    @Test
    void resolveOrProvision_createsUser_withSanitizedUsernameAndSuffix() {
        String sub = "550e8400-e29b-41d4-a716-446655440000";
        KeycloakIdentitySnapshot snapshot = new KeycloakIdentitySnapshot(sub, "John😊.Doe", "John", "Doe", "john@example.com", true);

        when(userQueryService.findByKeycloakSub(sub)).thenReturn(Optional.empty());
        when(userQueryService.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(userCommandService.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveOrProvision(snapshot);

        verify(userCommandService).create(userCaptor.capture());
        User passed = userCaptor.getValue();

        assertEquals("john.doe_550e8400", passed.getUsername());
        assertEquals(sub, passed.getKeycloakSub());
        assertEquals("john@example.com", passed.getEmail());
    }

    @Test
    void syncSnapshot_updatesOnlyWhenChanged_andUpdatesLastLoginAfterThreshold() {
        String sub = "550e8400-e29b-41d4-a716-446655440000";
        User existing = User.builder()
                .id(1L)
                .username("john.doe_550e8400")
                .email("john@example.com")
                .emailVerified(false)
                .keycloakSub(sub)
                .build();

        // last login 10 minutes ago => should be updated
        existing.setLastLoginAt(Instant.now().minus(10, ChronoUnit.MINUTES));

        KeycloakIdentitySnapshot snapshot = new KeycloakIdentitySnapshot(sub, null, null, null, "john@example.com", true);

        when(userQueryService.findByKeycloakSub(sub)).thenReturn(Optional.of(existing));
        when(userCommandService.update(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.resolveOrProvision(snapshot);

        verify(userCommandService, times(1)).update(userCaptor.capture());
        User updated = userCaptor.getValue();

        assertTrue(updated.isEmailVerified());
        assertNotNull(updated.getLastLoginAt());
        assertEquals(result.getLastLoginAt(), updated.getLastLoginAt());
    }

    @Test
    void linkExistingUser_throwsWhenAlreadyLinkedToDifferentSub() {
        String existingSub = "existing-sub-123";
        String newSub = "550e8400-e29b-41d4-a716-446655440000";

        User existing = User.builder()
                .id(2L)
                .email("jane@example.com")
                .keycloakSub(existingSub)
                .build();

        KeycloakIdentitySnapshot snapshot = new KeycloakIdentitySnapshot(newSub, null, null, null, "jane@example.com", true);

        when(userQueryService.findByKeycloakSub(newSub)).thenReturn(Optional.empty());
        when(userQueryService.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.resolveOrProvision(snapshot));

        verify(userCommandService, never()).create(any());
        verify(userCommandService, never()).update(any());
    }
}
