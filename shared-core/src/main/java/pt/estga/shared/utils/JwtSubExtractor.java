package pt.estga.shared.utils;

import java.util.Map;

public final class JwtSubExtractor {

    private JwtSubExtractor() {
    }

    public static String extractSub(Map<String, ?> claims) {
        if (claims == null) {
            throw new IllegalArgumentException("claims must not be null");
        }

        Object sub = claims.get("sub");
        if (!(sub instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("JWT 'sub' claim is missing or blank");
        }

        return value;
    }
}
