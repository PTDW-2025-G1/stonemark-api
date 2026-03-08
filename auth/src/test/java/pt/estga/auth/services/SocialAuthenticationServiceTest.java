package pt.estga.auth.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.auth.dtos.AuthenticationResponseDto;
import pt.estga.security.enums.TokenType;
import pt.estga.security.services.AccessTokenService;
import pt.estga.security.services.JwtService;
import pt.estga.security.services.RefreshTokenService;
import pt.estga.shared.enums.PrincipalType;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.entities.User;
import pt.estga.user.entities.UserIdentity;
import pt.estga.user.enums.Provider;
import pt.estga.user.enums.TfaMethod;
import pt.estga.user.services.UserIdentityService;
import pt.estga.user.services.UserService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialAuthenticationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock
    private JwtService jwtService;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private GoogleIdToken googleIdToken;
    @Mock
    private GoogleIdToken.Payload payload;

    @InjectMocks
    private SocialAuthenticationServiceImpl socialAuthenticationService;

    private static final String GOOGLE_TOKEN = "someGoogleToken";
    private static final String GOOGLE_ID = "googleSubId";
    private static final String USER_EMAIL = "test@example.com";
    private static final String FIRST_NAME = "Test";
    private static final String LAST_NAME = "User";

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .username(USER_EMAIL)
                .email(USER_EMAIL)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .role(UserRole.USER)
                .enabled(true)
                .tfaMethod(TfaMethod.NONE)
                .build();
    }

    @Test
    void authenticateWithGoogle_shouldReturnAuthResponse_whenNewUser() throws GeneralSecurityException, IOException {
        User newUser = createTestUser();
        UserIdentity newIdentity = UserIdentity.builder().provider(Provider.GOOGLE).value(GOOGLE_ID).user(newUser).build();

        when(googleIdTokenVerifier.verify(GOOGLE_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn(USER_EMAIL);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn(GOOGLE_ID);
        when(payload.get("given_name")).thenReturn(FIRST_NAME);
        when(payload.get("family_name")).thenReturn(LAST_NAME);
        when(userIdentityService.findByProviderAndValue(Provider.GOOGLE, GOOGLE_ID)).thenReturn(Optional.empty());
        when(userService.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(userService.create(any(User.class))).thenReturn(newUser);
        when(userIdentityService.createAndAssociate(any(User.class), eq(Provider.GOOGLE), eq(GOOGLE_ID))).thenReturn(newIdentity);
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(newUser.getId()), eq(newUser.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = socialAuthenticationService.authenticateWithGoogle(GOOGLE_TOKEN);

        assertTrue(response.isPresent());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());
        verify(userService).create(any(User.class));
        verify(userIdentityService).createAndAssociate(any(User.class), eq(Provider.GOOGLE), eq(GOOGLE_ID));
    }

    @Test
    void authenticateWithGoogle_shouldReturnAuthResponse_whenExistingIdentityUser() throws GeneralSecurityException, IOException {
        User existingUser = createTestUser();
        UserIdentity existingIdentity = UserIdentity.builder().provider(Provider.GOOGLE).value(GOOGLE_ID).user(existingUser).build();

        when(googleIdTokenVerifier.verify(GOOGLE_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn(USER_EMAIL);
        when(payload.getSubject()).thenReturn(GOOGLE_ID);
        when(userIdentityService.findByProviderAndValue(Provider.GOOGLE, GOOGLE_ID)).thenReturn(Optional.of(existingIdentity));
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(existingUser.getId()), eq(existingUser.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = socialAuthenticationService.authenticateWithGoogle(GOOGLE_TOKEN);

        assertTrue(response.isPresent());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());
        verify(userService, never()).create(any(User.class));
        verify(userIdentityService, never()).createAndAssociate(any(User.class), any(Provider.class), anyString());
    }

    @Test
    void authenticateWithGoogle_shouldLinkToExistingUser_whenEmailExists() throws GeneralSecurityException, IOException {
        User existingUser = createTestUser();
        UserIdentity newIdentity = UserIdentity.builder().provider(Provider.GOOGLE).value(GOOGLE_ID).user(existingUser).build();

        when(googleIdTokenVerifier.verify(GOOGLE_TOKEN)).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn(USER_EMAIL);
        when(payload.getSubject()).thenReturn(GOOGLE_ID);
        when(userIdentityService.findByProviderAndValue(Provider.GOOGLE, GOOGLE_ID)).thenReturn(Optional.empty());
        when(userService.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(userIdentityService.createAndAssociate(existingUser, Provider.GOOGLE, GOOGLE_ID)).thenReturn(newIdentity);
        when(jwtService.generateTokens(eq(PrincipalType.USER), eq(existingUser.getId()), eq(existingUser.getUsername()), any()))
                .thenReturn(Map.of(TokenType.ACCESS, "accessToken", TokenType.REFRESH, "refreshToken"));

        Optional<AuthenticationResponseDto> response = socialAuthenticationService.authenticateWithGoogle(GOOGLE_TOKEN);

        assertTrue(response.isPresent());
        assertEquals("accessToken", response.get().accessToken());
        assertEquals("refreshToken", response.get().refreshToken());
        verify(userService, never()).create(any(User.class));
        verify(userIdentityService).createAndAssociate(existingUser, Provider.GOOGLE, GOOGLE_ID);
    }

    @Test
    void authenticateWithGoogle_shouldReturnEmpty_whenInvalidGoogleToken() throws GeneralSecurityException, IOException {
        when(googleIdTokenVerifier.verify(GOOGLE_TOKEN)).thenReturn(null);

        Optional<AuthenticationResponseDto> response = socialAuthenticationService.authenticateWithGoogle(GOOGLE_TOKEN);

        assertFalse(response.isPresent());
        verifyNoInteractions(userService, userIdentityService, jwtService, accessTokenService, refreshTokenService);
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void authenticateWithGoogle_shouldThrowRuntimeException_whenExceptionOccurs(Class<Exception> exceptionClass) throws GeneralSecurityException, IOException {
        when(googleIdTokenVerifier.verify(GOOGLE_TOKEN)).thenThrow(exceptionClass);

        assertThrows(RuntimeException.class, () -> socialAuthenticationService.authenticateWithGoogle(GOOGLE_TOKEN));
    }

    static Stream<Class<? extends Exception>> exceptionProvider() {
        return Stream.of(GeneralSecurityException.class, IOException.class);
    }

    @Test
    void authenticateWithTelegram_shouldReturnEmptyAndLogWarning() {
        Optional<AuthenticationResponseDto> response = socialAuthenticationService.authenticateWithTelegram("someTelegramData");

        assertFalse(response.isPresent());
    }
}
