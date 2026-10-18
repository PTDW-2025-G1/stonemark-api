package pt.estga.shared.core.utils;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pt.estga.shared.core.interfaces.AuthenticatedPrincipal;
import pt.estga.shared.core.models.AppPrincipal;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Retrieves the ID of the currently authenticated user from the SecurityContext.
     *
     * @return An Optional containing the user ID if authenticated, or empty if not.
     */
    public static Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
            return Optional.ofNullable(authenticatedPrincipal.getId());
        }

        return Optional.empty();
    }

    /**
     * Retrieves the currently authenticated principal from the SecurityContext.
     *
     * @return An Optional containing the AppPrincipal if authenticated, or empty if not.
     */
    public static Optional<AppPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (!(auth.getPrincipal() instanceof AppPrincipal p)) return Optional.empty();
        return Optional.of(p);
    }

}
