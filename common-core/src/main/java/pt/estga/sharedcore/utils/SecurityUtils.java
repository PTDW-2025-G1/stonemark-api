package pt.estga.sharedcore.utils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pt.estga.sharedcore.interfaces.AuthenticatedPrincipal;
import pt.estga.sharedcore.models.AppPrincipal;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Retrieves the ID of the currently authenticated user from the SecurityContext.
     *
     * @return An Optional containing the user ID if authenticated, or empty if not.
     */
    public static Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
            return Optional.of(authenticatedPrincipal.getId());
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
