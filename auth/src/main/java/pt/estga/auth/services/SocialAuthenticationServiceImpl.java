package pt.estga.auth.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.auth.dtos.AuthenticationResponseDto;
import pt.estga.auth.utils.LegacyJwtAuthHelper;
import pt.estga.security.services.AccessTokenService;
import pt.estga.security.services.JwtService;
import pt.estga.security.services.RefreshTokenService;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.entities.User;
import pt.estga.user.entities.UserIdentity;
import pt.estga.user.enums.Provider;
import pt.estga.user.enums.TfaMethod;
import pt.estga.user.services.UserIdentityService;
import pt.estga.user.services.UserService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthenticationServiceImpl implements SocialAuthenticationService {

    private final UserService userService;
    private final UserIdentityService userIdentityService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final JwtService jwtService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public Optional<AuthenticationResponseDto> authenticateWithGoogle(String token) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(token);
            if (idToken == null) {
                return Optional.empty();
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            User user = upsertUserFromGooglePayload(payload);

            return LegacyJwtAuthHelper.generateAuthResponse(
                    user, false, false, jwtService, refreshTokenService, accessTokenService
            );
        } catch (GeneralSecurityException | IOException e) {
            log.error("Error while authenticating with Google", e);
            throw new RuntimeException("Google authentication failed.", e);
        }
    }

    private User upsertUserFromGooglePayload(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String googleId = payload.getSubject();

        return userIdentityService.findByProviderAndValue(Provider.GOOGLE, googleId)
                .map(UserIdentity::getUser)
                .orElseGet(() -> userService.findByEmail(email)
                        .orElseGet(() -> {
                            String firstName = (String) payload.get("given_name");
                            String lastName = (String) payload.get("family_name");
                            String username = generateUniqueUsername(firstName, lastName);

                            User newUser = User.builder()
                                    .username(username)
                                    .firstName(firstName)
                                    .lastName(lastName)
                                    .email(email)
                                    .emailVerified(Boolean.TRUE.equals(payload.getEmailVerified()))
                                    .role(UserRole.USER)
                                    .enabled(true)
                                    .tfaMethod(TfaMethod.NONE)
                                    .password(null)
                                    .build();

                            User createdUser = userService.create(newUser);
                            userIdentityService.createAndAssociate(createdUser, Provider.GOOGLE, googleId);
                            return createdUser;
                        }));
    }

    private String generateUniqueUsername(String firstName, String lastName) {
        String normalizedFirst = firstName == null ? "user" : firstName;
        String normalizedLast = lastName == null ? "social" : lastName;
        String baseUsername = (normalizedFirst + normalizedLast).toLowerCase().replaceAll("\\s+", "");
        String username = baseUsername;
        int counter = 1;
        while (userService.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        return username;
    }

    @Override
    public Optional<AuthenticationResponseDto> authenticateWithTelegram(String telegramData) {
        // TODO: Implement Telegram authentication logic here
        log.warn("Telegram authentication not yet implemented.");
        return Optional.empty();
    }
}
