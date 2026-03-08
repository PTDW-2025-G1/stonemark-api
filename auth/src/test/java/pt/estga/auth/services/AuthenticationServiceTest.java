package pt.estga.auth.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.estga.auth.dtos.AuthenticationResponseDto;
import pt.estga.auth.services.tfa.TotpService;
import pt.estga.auth.services.tfa.TwoFactorAuthenticationService;
import pt.estga.security.entities.RefreshToken;
import pt.estga.security.enums.TokenType;
import pt.estga.security.services.AccessTokenService;
import pt.estga.security.services.JwtService;
import pt.estga.security.services.RefreshTokenService;
import pt.estga.shared.enums.PrincipalType;
import pt.estga.shared.enums.UserRole;
import pt.estga.shared.exceptions.InvalidCredentialsException;
import pt.estga.shared.exceptions.UsernameAlreadyTakenException;
import pt.estga.user.entities.User;
import pt.estga.user.enums.TfaMethod;
import pt.estga.user.services.UserService;
import pt.estga.verification.enums.ActionCodeType;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TotpService totpService;

    @Mock
    private TwoFactorAuthenticationService twoFactorAuthenticationService;

    @InjectMocks
    private AuthenticationServiceSpringImpl authenticationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("password");
        user.setUsername("user228");
        user.setEmail("test@example.com");
        user.setRole(UserRole.USER);
        user.setTfaMethod(TfaMethod.NONE);
    }

    @Test
    void register_shouldCreateUserAndReturnAuthResponse_whenGivenValidData() {
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userService.create(any(User.class))).thenReturn(user);
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(user.getId()), eq(user.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = authenticationService.register(user);

        assertTrue(response.isPresent());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());

        verify(userService).create(
                argThat(u ->
                        u.getUsername().equals("user228") &&
                        u.getFirstName().equals("Test") &&
                        u.getLastName().equals("User") &&
                        u.getPassword().equals("encodedPassword"))
        );
    }

    @Test
    void register_shouldThrowUsernameAlreadyTakenException_whenUsernameExists() {
        when(userService.existsByUsername(user.getUsername())).thenReturn(true);

        assertThrows(UsernameAlreadyTakenException.class, () -> authenticationService.register(user));
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void authenticate_shouldReturnAuthenticationResponse_whenCredentialsAreValid() {
        user.setTfaMethod(TfaMethod.NONE);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(user.getId()), eq(user.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = authenticationService.authenticate("test@example.com", "password", null);

        assertTrue(response.isPresent());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());
        assertFalse(response.get().tfaRequired());
        assertFalse(response.get().tfaCodeSent());
        verify(authenticationManager).authenticate(any());
        verify(accessTokenService).createToken(any(), any(), any());
        verify(refreshTokenService).createToken(any(), any());
    }

    @Test
    void authenticate_shouldThrowInvalidCredentials_whenCredentialsAreInvalid() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.authenticate("test@example.com", "wrongpassword", null));
    }

    @Test
    void authenticate_shouldReturnTfaRequired_whenTfaTotpIsEnabledAndNoCodeProvided() {
        user.setTfaMethod(TfaMethod.TOTP);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<AuthenticationResponseDto> response = authenticationService.authenticate("test@example.com", "password", null);

        assertTrue(response.isPresent());
        assertTrue(response.get().tfaRequired());
        assertFalse(response.get().tfaCodeSent());
        assertNull(response.get().accessToken());
        assertNull(response.get().refreshToken());
    }

    @Test
    void authenticate_shouldReturnTfaRequiredAndCodeSent_whenTfaSmsIsEnabledAndNoCodeProvided() {
        user.setTfaMethod(TfaMethod.SMS);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<AuthenticationResponseDto> response = authenticationService.authenticate("test@example.com", "password", null);

        assertTrue(response.isPresent());
        assertTrue(response.get().tfaRequired());
        assertTrue(response.get().tfaCodeSent());
        assertNull(response.get().accessToken());
        assertNull(response.get().refreshToken());
        verify(twoFactorAuthenticationService).generateAndSendSmsCode(user);
    }

    @Test
    void authenticate_shouldReturnTfaRequiredAndCodeSent_whenTfaEmailIsEnabledAndNoCodeProvided() {
        user.setTfaMethod(TfaMethod.EMAIL);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<AuthenticationResponseDto> response = authenticationService.authenticate("test@example.com", "password", null);

        assertTrue(response.isPresent());
        assertTrue(response.get().tfaRequired());
        assertTrue(response.get().tfaCodeSent());
        assertNull(response.get().accessToken());
        assertNull(response.get().refreshToken());
        verify(twoFactorAuthenticationService).generateAndSendEmailCode(user);
    }

    @Test
    void authenticate_shouldReturnAuthenticationResponse_whenTfaTotpIsEnabledAndCodeIsValid() {
        user.setTfaMethod(TfaMethod.TOTP);
        user.setTfaSecret("secret");

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(totpService.isCodeValid("secret", "123456")).thenReturn(true);
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(user.getId()), eq(user.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = authenticationService.authenticate("test@example.com", "password", "123456");

        assertTrue(response.isPresent());
        assertFalse(response.get().tfaRequired());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());
    }

    @Test
    void authenticate_shouldReturnAuthenticationResponse_whenTfaSmsIsEnabledAndCodeIsValid() {
        user.setTfaMethod(TfaMethod.SMS);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(twoFactorAuthenticationService.verifyCode(user, "123456", ActionCodeType.TWO_FACTOR)).thenReturn(true);
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(user.getId()), eq(user.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = authenticationService.authenticate("test@example.com", "password", "123456");

        assertTrue(response.isPresent());
        assertFalse(response.get().tfaRequired());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());
    }

    @Test
    void authenticate_shouldReturnAuthenticationResponse_whenTfaEmailIsEnabledAndCodeIsValid() {
        user.setTfaMethod(TfaMethod.EMAIL);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(twoFactorAuthenticationService.verifyCode(user, "123456", ActionCodeType.TWO_FACTOR)).thenReturn(true);
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(user.getId()), eq(user.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = authenticationService.authenticate("test@example.com", "password", "123456");

        assertTrue(response.isPresent());
        assertFalse(response.get().tfaRequired());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());
    }

    @Test
    void authenticate_shouldThrowInvalidCredentials_whenTfaTotpIsEnabledAndCodeIsInvalid() {
        user.setTfaMethod(TfaMethod.TOTP);
        user.setTfaSecret("secret");

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(totpService.isCodeValid("secret", "wrong-code")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.authenticate("test@example.com", "password", "wrong-code"));
    }

    @Test
    void authenticate_shouldThrowInvalidCredentials_whenTfaSmsIsEnabledAndCodeIsInvalid() {
        user.setTfaMethod(TfaMethod.SMS);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(twoFactorAuthenticationService.verifyCode(user, "wrong-code", ActionCodeType.TWO_FACTOR)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.authenticate("test@example.com", "password", "wrong-code"));
    }

    @Test
    void authenticate_shouldThrowInvalidCredentials_whenTfaEmailIsEnabledAndCodeIsInvalid() {
        user.setTfaMethod(TfaMethod.EMAIL);

        when(userService.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(twoFactorAuthenticationService.verifyCode(user, "wrong-code", ActionCodeType.TWO_FACTOR)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.authenticate("test@example.com", "password", "wrong-code"));
    }

    @Test
    void refreshToken_shouldReturnNewAccessToken_whenRefreshTokenIsValid() {
        String refreshTokenString = "validRefreshToken";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenString);

        when(refreshTokenService.findByToken(refreshTokenString)).thenReturn(Optional.of(refreshToken));
        when(jwtService.isTokenValid(refreshTokenString, TokenType.REFRESH)).thenReturn(true);
        when(userService.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(eq(PrincipalType.USER), eq(user.getId()), eq(user.getUsername()), any())).thenReturn("newAccessToken");

        Optional<AuthenticationResponseDto> response = authenticationService.refreshToken(refreshTokenString);

        assertTrue(response.isPresent());
        assertEquals("newAccessToken", response.get().accessToken());
        assertEquals(refreshTokenString, response.get().refreshToken());
        assertFalse(response.get().tfaRequired());
        assertFalse(response.get().tfaCodeSent());
        assertEquals(user.getRole().name(), response.get().role());
        verify(accessTokenService).revokeAllByRefreshToken(refreshToken);
        verify(accessTokenService).createToken(user.getId(), "newAccessToken", refreshToken);
    }

    @Test
    void refreshToken_shouldReturnEmpty_whenRefreshTokenIsInvalid() {
        String refreshTokenString = "invalidRefreshToken";

        when(refreshTokenService.findByToken(refreshTokenString)).thenReturn(Optional.empty());

        Optional<AuthenticationResponseDto> response = authenticationService.refreshToken(refreshTokenString);

        assertFalse(response.isPresent());
    }

    @Test
    void logoutFromAllDevices_shouldRevokeAllTokensForUser() {
        authenticationService.logoutFromAllDevices(user);

        verify(refreshTokenService).revokeAllByUserId(user.getId());
        verify(accessTokenService).revokeAllByUserId(user.getId());
    }
}

