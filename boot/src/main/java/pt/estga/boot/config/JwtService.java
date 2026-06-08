package pt.estga.boot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import pt.estga.user.entities.User;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    private static final long ACCESS_TOKEN_TTL_SECONDS = 3600;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 14 * 24 * 3600;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("stonemark-api")
                .subject(String.valueOf(user.getId()))
                .claim("preferred_username", user.getUsername())
                .claim("token_version", user.getTokenVersion())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS))
                .id(UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("stonemark-api")
                .subject(String.valueOf(user.getId()))
                .claim("preferred_username", user.getUsername())
                .claim("token_version", user.getTokenVersion())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(REFRESH_TOKEN_TTL_SECONDS))
                .id(UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public Jwt validateAndParse(String token) {
        return jwtDecoder.decode(token);
    }
}
