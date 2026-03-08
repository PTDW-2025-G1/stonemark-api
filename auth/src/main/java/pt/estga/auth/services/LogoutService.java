package pt.estga.auth.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

/**
 * Logout handler for Keycloak-authenticated sessions.
 *
 * Note: Keycloak uses stateless JWT tokens. True logout requires:
 * 1. Frontend clears tokens from storage
 * 2. Backend clears SecurityContext (done here)
 * 3. For session revocation, redirect to Keycloak logout endpoint:
 *    GET {keycloak}/realms/{realm}/protocol/openid-connect/logout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService implements LogoutHandler {

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        log.debug("User logged out, SecurityContext cleared");

        // Note: For full Keycloak logout (session revocation), frontend should redirect to:
        // http://localhost:8081/realms/stonemark/protocol/openid-connect/logout
        // with parameters: client_id, post_logout_redirect_uri, id_token_hint (optional)
    }
}
