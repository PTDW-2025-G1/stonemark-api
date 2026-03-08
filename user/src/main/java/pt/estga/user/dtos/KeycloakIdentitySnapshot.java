package pt.estga.user.dtos;

import pt.estga.shared.utils.JwtSubExtractor;

import java.util.Map;

public record KeycloakIdentitySnapshot(
        String sub,
        String preferredUsername,
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified
) {

    public static KeycloakIdentitySnapshot fromClaims(Map<String, ?> claims) {
        String sub = JwtSubExtractor.extractSub(claims);
        String preferredUsername = asString(claims.get("preferred_username"));
        String email = asString(claims.get("email"));
        boolean emailVerified = asBoolean(claims.get("email_verified"));
        String phone = asString(claims.get("phone_number"));
        boolean phoneVerified = asBoolean(claims.get("phone_number_verified"));

        return new KeycloakIdentitySnapshot(sub, preferredUsername, email, emailVerified, phone, phoneVerified);
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

