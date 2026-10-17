package pt.estga.user.enums;

/**
 * The single role a user holds, ordered from least to most privileged.
 *
 * <p>Capabilities are inherited through the security role hierarchy
 * ({@code ADMIN > MODERATOR > REVIEWER > USER}), so an endpoint only needs to
 * declare the minimum role it requires.
 */
public enum UserRole {
    USER,
    REVIEWER,
    MODERATOR,
    ADMIN
}
