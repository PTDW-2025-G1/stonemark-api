package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.estga.user.entities.User;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;
import pt.estga.verification.repositories.ActionCodeRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionCodeServiceTest {

    @Mock
    private ActionCodeRepository actionCodeRepository;

    @InjectMocks
    private ActionCodeServiceImpl actionCodeService;

    private User testUser;
    private String recipient;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        recipient = "test@example.com";

        ReflectionTestUtils.setField(actionCodeService, "emailVerificationExpiration", 3600000L);
        ReflectionTestUtils.setField(actionCodeService, "passwordResetExpiration", 1800000L);
        ReflectionTestUtils.setField(actionCodeService, "twoFactorExpiration", 300000L);
        ReflectionTestUtils.setField(actionCodeService, "telephoneVerificationExpiration", 600000L);
        ReflectionTestUtils.setField(actionCodeService, "deviceVerificationExpiration", 86400000L);
        ReflectionTestUtils.setField(actionCodeService, "chatbotVerificationExpiration", 900000L);
    }

    @Test
    void createAndSave_shouldCreateAndSaveNewCode_whenNoExistingCode() {
        when(actionCodeRepository.findByUserAndType(testUser, ActionCodeType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(actionCodeRepository.save(any(ActionCode.class)))
                .thenAnswer(invocation -> {
                    ActionCode ac = invocation.getArgument(0);
                    ReflectionTestUtils.setField(ac, "id", 1L);
                    return ac;
                });

        ActionCode createdCode = actionCodeService.createAndSave(testUser, recipient, ActionCodeType.EMAIL_VERIFICATION);

        assertNotNull(createdCode);
        assertEquals(testUser, createdCode.getUser());
        assertEquals(recipient, createdCode.getRecipient());
        assertEquals(ActionCodeType.EMAIL_VERIFICATION, createdCode.getType());
        assertFalse(createdCode.isConsumed());
        assertNotNull(createdCode.getCode());
        assertEquals(6, createdCode.getCode().length());
        assertTrue(createdCode.getCode().matches("[A-Z0-9]+"));
        assertTrue(createdCode.getExpiresAt().isAfter(Instant.now().plus(59, ChronoUnit.MINUTES)));
        verify(actionCodeRepository, never()).delete(any(ActionCode.class));
        verify(actionCodeRepository, times(1)).save(any(ActionCode.class));
    }

    @Test
    void createAndSave_shouldDeleteExistingCodeAndCreateNewOne() {
        ActionCode existingCode = ActionCode.builder()
                .id(10L)
                .user(testUser)
                .type(ActionCodeType.EMAIL_VERIFICATION)
                .code("OLDCODE")
                .consumed(false)
                .expiresAt(Instant.now().minusSeconds(100))
                .build();

        when(actionCodeRepository.findByUserAndType(testUser, ActionCodeType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(existingCode));
        when(actionCodeRepository.save(any(ActionCode.class)))
                .thenAnswer(invocation -> {
                    ActionCode ac = invocation.getArgument(0);
                    ReflectionTestUtils.setField(ac, "id", 2L);
                    return ac;
                });

        ActionCode createdCode = actionCodeService.createAndSave(testUser, recipient, ActionCodeType.EMAIL_VERIFICATION);

        assertNotNull(createdCode);
        assertEquals(recipient, createdCode.getRecipient());
        assertNotEquals(existingCode.getCode(), createdCode.getCode());
        verify(actionCodeRepository, times(1)).delete(existingCode);
        verify(actionCodeRepository, times(1)).save(any(ActionCode.class));
    }

    @Test
    void createAndSave_shouldSetCorrectExpirationForTwoFactor() {
        when(actionCodeRepository.findByUserAndType(testUser, ActionCodeType.TWO_FACTOR))
                .thenReturn(Optional.empty());
        when(actionCodeRepository.save(any(ActionCode.class)))
                .thenAnswer(invocation -> {
                    ActionCode ac = invocation.getArgument(0);
                    ReflectionTestUtils.setField(ac, "id", 4L);
                    return ac;
                });

        ActionCode createdCode = actionCodeService.createAndSave(testUser, recipient, ActionCodeType.TWO_FACTOR);

        assertNotNull(createdCode);
        assertEquals(ActionCodeType.TWO_FACTOR, createdCode.getType());
        assertTrue(createdCode.getExpiresAt().isAfter(Instant.now().plus(4, ChronoUnit.MINUTES)));
        assertTrue(createdCode.getExpiresAt().isBefore(Instant.now().plus(6, ChronoUnit.MINUTES)));
    }

    @Test
    void createAndSave_shouldSetCorrectExpirationForPhoneVerification() {
        when(actionCodeRepository.findByUserAndType(testUser, ActionCodeType.PHONE_VERIFICATION))
                .thenReturn(Optional.empty());
        when(actionCodeRepository.save(any(ActionCode.class)))
                .thenAnswer(invocation -> {
                    ActionCode ac = invocation.getArgument(0);
                    ReflectionTestUtils.setField(ac, "id", 5L);
                    return ac;
                });

        ActionCode createdCode = actionCodeService.createAndSave(testUser, recipient, ActionCodeType.PHONE_VERIFICATION);

        assertNotNull(createdCode);
        assertEquals(ActionCodeType.PHONE_VERIFICATION, createdCode.getType());
        assertTrue(createdCode.getExpiresAt().isAfter(Instant.now().plus(9, ChronoUnit.MINUTES)));
        assertTrue(createdCode.getExpiresAt().isBefore(Instant.now().plus(11, ChronoUnit.MINUTES)));
    }

    @Test
    void createAndSave_shouldSetCorrectExpirationForDeviceVerification() {
        when(actionCodeRepository.findByUserAndType(testUser, ActionCodeType.DEVICE_VERIFICATION))
                .thenReturn(Optional.empty());
        when(actionCodeRepository.save(any(ActionCode.class)))
                .thenAnswer(invocation -> {
                    ActionCode ac = invocation.getArgument(0);
                    ReflectionTestUtils.setField(ac, "id", 6L);
                    return ac;
                });

        ActionCode createdCode = actionCodeService.createAndSave(testUser, recipient, ActionCodeType.DEVICE_VERIFICATION);

        assertNotNull(createdCode);
        assertEquals(ActionCodeType.DEVICE_VERIFICATION, createdCode.getType());
        assertTrue(createdCode.getExpiresAt().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));
        assertTrue(createdCode.getExpiresAt().isBefore(Instant.now().plus(25, ChronoUnit.HOURS)));
    }

    @Test
    void createAndSave_shouldPropagateException_whenRepositoryFails() {
        when(actionCodeRepository.findByUserAndType(testUser, ActionCodeType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(actionCodeRepository.save(any(ActionCode.class)))
                .thenThrow(new RuntimeException("DB error"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> actionCodeService.createAndSave(testUser, recipient, ActionCodeType.EMAIL_VERIFICATION));

        assertEquals("DB error", thrown.getMessage());
        verify(actionCodeRepository, times(1)).save(any(ActionCode.class));
    }

    @Test
    void findByCode_shouldReturnActionCode_whenFoundAndNotConsumed() {
        ActionCode actionCode = ActionCode.builder()
                .code("VALID123")
                .consumed(false)
                .build();
        when(actionCodeRepository.findAll()).thenReturn(List.of(actionCode));

        Optional<ActionCode> foundCode = actionCodeService.findByCode("VALID123");

        assertTrue(foundCode.isPresent());
        assertEquals("VALID123", foundCode.get().getCode());
    }

    @Test
    void findByCode_shouldReturnEmpty_whenNotFound() {
        when(actionCodeRepository.findAll()).thenReturn(List.of());

        Optional<ActionCode> foundCode = actionCodeService.findByCode("NONEXISTENT");

        assertTrue(foundCode.isEmpty());
    }

    @Test
    void findByCode_shouldReturnEmpty_whenFoundButConsumed() {
        ActionCode actionCode = ActionCode.builder()
                .code("CONSUMED")
                .consumed(true)
                .build();
        when(actionCodeRepository.findAll()).thenReturn(List.of(actionCode));

        Optional<ActionCode> foundCode = actionCodeService.findByCode("CONSUMED");

        assertTrue(foundCode.isEmpty());
    }

    @Test
    void isCodeValid_shouldReturnTrue_whenValidUnexpiredUnconsumed() {
        ActionCode actionCode = ActionCode.builder()
                .code("VALIDCODE")
                .consumed(false)
                .expiresAt(Instant.now().plusSeconds(100))
                .build();
        when(actionCodeRepository.findAll()).thenReturn(List.of(actionCode));

        assertTrue(actionCodeService.isCodeValid("VALIDCODE"));
    }

    @Test
    void isCodeValid_shouldReturnFalse_whenExpired() {
        ActionCode actionCode = ActionCode.builder()
                .code("EXPIRED")
                .consumed(false)
                .expiresAt(Instant.now().minusSeconds(100))
                .build();
        when(actionCodeRepository.findAll()).thenReturn(List.of(actionCode));

        assertFalse(actionCodeService.isCodeValid("EXPIRED"));
    }

    @Test
    void isCodeValid_shouldReturnFalse_whenConsumed() {
        ActionCode actionCode = ActionCode.builder()
                .code("CONSUMED")
                .consumed(true)
                .expiresAt(Instant.now().plusSeconds(100))
                .build();
        when(actionCodeRepository.findAll()).thenReturn(List.of(actionCode));

        assertFalse(actionCodeService.isCodeValid("CONSUMED"));
    }

    @Test
    void isCodeValid_shouldReturnFalse_whenNotFound() {
        when(actionCodeRepository.findAll()).thenReturn(List.of());

        assertFalse(actionCodeService.isCodeValid("NONEXISTENT"));
    }

    @Test
    void consumeCode_shouldSetConsumedTrueAndSave() {
        ActionCode actionCode = ActionCode.builder()
                .id(1L)
                .code("TOBECOMSUMED")
                .consumed(false)
                .build();

        actionCodeService.consumeCode(actionCode);

        assertTrue(actionCode.isConsumed());
        verify(actionCodeRepository, times(1)).save(actionCode);
    }
}
