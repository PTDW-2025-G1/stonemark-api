package pt.estga.verification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.shared.exceptions.InvalidTokenException;
import pt.estga.shared.exceptions.TokenExpiredException;
import pt.estga.shared.exceptions.TokenRevokedException;
import pt.estga.shared.exceptions.VerificationErrorMessages; // Import VerificationErrorMessages
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.enums.ActionCodeType;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionCodeValidationServiceTest {

    @Mock
    private ActionCodeService actionCodeService;

    @InjectMocks
    private ActionCodeValidationServiceImpl actionCodeValidationService;

    private ActionCode validActionCode;
    private final String TEST_CODE = "TESTCODE123";

    @BeforeEach
    void setUp() {
        validActionCode = ActionCode.builder()
                .code(TEST_CODE)
                .type(ActionCodeType.TELEGRAM_VERIFICATION)
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour from now
                .consumed(false)
                .build();
    }

    @Test
    void getValidatedActionCode_shouldReturnActionCode_whenCodeIsValid() {
        when(actionCodeService.findByCode(TEST_CODE)).thenReturn(Optional.of(validActionCode));

        ActionCode result = actionCodeValidationService.getValidatedActionCode(TEST_CODE);

        assertNotNull(result);
        assertEquals(TEST_CODE, result.getCode());
        verify(actionCodeService, times(1)).findByCode(TEST_CODE);
        verify(actionCodeService, never()).consumeCode(any(ActionCode.class));
    }

    @Test
    void getValidatedActionCode_shouldThrowInvalidTokenException_whenCodeNotFound() {
        when(actionCodeService.findByCode(TEST_CODE)).thenReturn(Optional.empty());

        InvalidTokenException thrown = assertThrows(InvalidTokenException.class,
                () -> actionCodeValidationService.getValidatedActionCode(TEST_CODE));

        assertEquals(VerificationErrorMessages.CODE_NOT_FOUND, thrown.getMessage());
        verify(actionCodeService, times(1)).findByCode(TEST_CODE);
        verify(actionCodeService, never()).consumeCode(any(ActionCode.class));
    }

    @Test
    void getValidatedActionCode_shouldThrowTokenExpiredException_whenCodeIsExpired() {
        validActionCode.setExpiresAt(Instant.now().minusSeconds(3600)); // 1 hour ago
        when(actionCodeService.findByCode(TEST_CODE)).thenReturn(Optional.of(validActionCode));

        TokenExpiredException thrown = assertThrows(TokenExpiredException.class,
                () -> actionCodeValidationService.getValidatedActionCode(TEST_CODE));

        assertEquals(VerificationErrorMessages.CODE_EXPIRED, thrown.getMessage());
        verify(actionCodeService, times(1)).findByCode(TEST_CODE);
        verify(actionCodeService, times(1)).consumeCode(validActionCode);
    }

    @Test
    void getValidatedActionCode_shouldThrowTokenRevokedException_whenCodeIsConsumed() {
        validActionCode.setConsumed(true);
        when(actionCodeService.findByCode(TEST_CODE)).thenReturn(Optional.of(validActionCode));

        TokenRevokedException thrown = assertThrows(TokenRevokedException.class,
                () -> actionCodeValidationService.getValidatedActionCode(TEST_CODE));

        assertEquals(VerificationErrorMessages.CODE_REVOKED, thrown.getMessage());
        verify(actionCodeService, times(1)).findByCode(TEST_CODE);
        verify(actionCodeService, never()).consumeCode(any(ActionCode.class));
    }
}
