package pt.estga.shared.utils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import pt.estga.shared.legacy.UserRole;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.shared.models.AppPrincipal;

import java.util.Collection;
import java.util.List;
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

    @Deprecated(forRemoval = true)
    public static Collection<? extends GrantedAuthority> mapUserRolesToAuthorities(UserRole role) {
        return switch (role) {
            case ADMIN -> List.of(
                    () -> UserRole.USER.name(),
                    () -> UserRole.REVIEWER.name(),
                    () -> UserRole.MODERATOR.name(),
                    () -> UserRole.ADMIN.name()
            );
            case MODERATOR -> List.of(
                    () -> UserRole.USER.name(),
                    () -> UserRole.REVIEWER.name(),
                    () -> UserRole.MODERATOR.name()
            );
            case REVIEWER -> List.of(
                    () -> UserRole.USER.name(),
                    () -> UserRole.REVIEWER.name()
            );
            case USER -> List.of(
                    () -> UserRole.USER.name()
            );
        };
    }
}
