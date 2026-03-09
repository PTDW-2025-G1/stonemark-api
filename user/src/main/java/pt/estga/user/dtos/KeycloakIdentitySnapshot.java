package pt.estga.user.dtos;

import pt.estga.shared.utils.JwtSubExtractor;

import java.util.Map;

/**
 * Snapshot of user identity data extracted from Keycloak JWT token.
 * These are the claims that get synced to the local User entity during JIT provisioning.
 */
public record KeycloakIdentitySnapshot(
        String sub,
        String preferredUsername,
        String givenName,
        String familyName,
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified
) {

    public static KeycloakIdentitySnapshot fromClaims(Map<String, ?> claims) {
        String sub = JwtSubExtractor.extractSub(claims);
        String preferredUsername = asString(claims.get("preferred_username"));
        String givenName = asString(claims.get("given_name"));
        String familyName = asString(claims.get("family_name"));
        String email = asString(claims.get("email"));
        boolean emailVerified = asBoolean(claims.get("email_verified"));
        String phone = asString(claims.get("phone_number"));
        boolean phoneVerified = asBoolean(claims.get("phone_number_verified"));

        return new KeycloakIdentitySnapshot(
                sub,
                preferredUsername,
                givenName,
                familyName,
                email,
                emailVerified,
                phone,
                phoneVerified
        );
    }

    private static String asString(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return false;
    }
}

