package pt.estga.auth.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pt.estga.auth.dtos.AuthenticationResponseDto;
import pt.estga.security.enums.TokenType;
import pt.estga.security.services.AccessTokenService;
import pt.estga.security.services.JwtService;
import pt.estga.security.services.RefreshTokenService;
import pt.estga.shared.enums.PrincipalType;
import pt.estga.user.entities.User;
import pt.estga.user.enums.TfaMethod;

import java.util.Collections;
import java.util.Optional;

/**
 * Utility class for generating legacy JWT authentication responses.
 * Used only by social authentication (Google/Telegram) until they are migrated to Keycloak.
 *
 * @deprecated Social auth should use Keycloak. This will be removed when social auth is migrated.
 */
@Deprecated
public final class LegacyJwtAuthHelper {

    private LegacyJwtAuthHelper() {
    }

    @NotNull
    public static Optional<AuthenticationResponseDto> generateAuthResponse(
            User user,
            boolean tfaRequired,
            boolean tfaCodeSent,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AccessTokenService accessTokenService
    ) {
        if (tfaRequired) {
            return Optional.of(new AuthenticationResponseDto(
                    null,
                    null,
                    user.getRole().name(),
                    user.getTfaMethod() != TfaMethod.NONE,
                    true,
                    tfaCodeSent
            ));
        }

        var tokens = jwtService.generateTokens(
                PrincipalType.USER,
                user.getId(),
                user.getUsername(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );

        var accessTokenString = tokens.get(TokenType.ACCESS);
        var refreshTokenString = tokens.get(TokenType.REFRESH);

        var refreshToken = refreshTokenService.createToken(user.getId(), refreshTokenString);
        accessTokenService.createToken(user.getId(), accessTokenString, refreshToken);

        return Optional.of(new AuthenticationResponseDto(
                accessTokenString,
                refreshTokenString,
                user.getRole().name(),
                user.getTfaMethod() != TfaMethod.NONE,
                false,
                tfaCodeSent
        ));
    }
}
