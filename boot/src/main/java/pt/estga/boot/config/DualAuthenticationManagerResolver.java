package pt.estga.boot.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import pt.estga.security.enums.TokenType;
import pt.estga.security.services.JwtService;
import pt.estga.shared.models.AppPrincipal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DualAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final JwtDecoder keycloakJwtDecoder;
    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;
    private final JwtService legacyJwtService;
    private final AppPrincipalFactory principalFactory;

    @Value("${application.security.keycloak.enabled:false}")
    private boolean keycloakEnabled;

    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        if (!keycloakEnabled) {
            return createLegacyAuthenticationManager(request);
        }

        return authentication -> {
            if (!(authentication instanceof BearerTokenAuthenticationToken bearerToken)) {
                throw new AuthenticationException("Unsupported authentication type") {};
            }

            String token = bearerToken.getToken();

            // Try Keycloak validation first (full signature validation)
            try {
                Jwt jwt = keycloakJwtDecoder.decode(token);
                Authentication result = keycloakJwtAuthenticationConverter.convert(jwt);
                log.debug("Successfully authenticated with Keycloak JWT");
                return result;
            } catch (JwtException e) {
                log.debug("Keycloak validation failed, falling back to legacy: {}", e.getMessage());

                // Fallback to legacy validator
                try {
                    return authenticateLegacy(token, request);
                } catch (Exception legacyEx) {
                    log.warn("Both Keycloak and legacy validation failed");
                    throw new AuthenticationException("Token validation failed") {};
                }
            }
        };
    }

    private AuthenticationManager createLegacyAuthenticationManager(HttpServletRequest request) {
        return authentication -> {
            if (!(authentication instanceof BearerTokenAuthenticationToken bearerToken)) {
                throw new AuthenticationException("Unsupported authentication type") {};
            }
            return authenticateLegacy(bearerToken.getToken(), request);
        };
    }

    private Authentication authenticateLegacy(String token, HttpServletRequest request) {
        boolean isRefreshRequest = request.getRequestURI().endsWith("/refresh");
        TokenType expectedType = isRefreshRequest ? TokenType.REFRESH : TokenType.ACCESS;

        if (!legacyJwtService.isTokenValid(token, expectedType)) {
            throw new AuthenticationException("Legacy token invalid") {};
        }

        var principalType = legacyJwtService.getPrincipalType(token);
        var principalId = legacyJwtService.getPrincipalId(token);
        var identifier = legacyJwtService.getSubject(token);
        var authorities = legacyJwtService.getAuthorities(token);

        AppPrincipal principal = switch (principalType) {
            case USER -> principalFactory.fromJwtUser(principalId, identifier, authorities);
            case SERVICE -> principalFactory.fromJwtService(principalId, identifier, authorities);
        };

        log.debug("Successfully authenticated with legacy JWT");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}

