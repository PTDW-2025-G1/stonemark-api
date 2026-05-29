package pt.estga.boot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.text.ParseException;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class ApplicationConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String keycloakJwkSetUri;

    @Value("${spring.security.oauth2.authorizationserver.issuer:http://localhost:8080}")
    private String sasIssuer;

    private final JWKSource<SecurityContext> jwkSource;

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder keycloakJwtDecoder() {
        if (keycloakJwkSetUri == null || keycloakJwkSetUri.isBlank()) {
            return jwt -> {
                throw new JwtException("Keycloak JWK Set URI not configured");
            };
        }
        return NimbusJwtDecoder.withJwkSetUri(keycloakJwkSetUri).build();
    }

    @Bean
    public JwtDecoder sasJwtDecoder() {
        return NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    @Primary
    @Bean
    @Deprecated(forRemoval = true)
    public JwtDecoder delegatingJwtDecoder(
            JwtDecoder keycloakJwtDecoder,
            JwtDecoder sasJwtDecoder) {
        return token -> {
            String issuer;
            try {
                issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
            } catch (ParseException e) {
                throw new JwtException("Failed to parse JWT for issuer routing", e);
            }
            if (issuer != null && issuer.startsWith(sasIssuer)) {
                return sasJwtDecoder.decode(token);
            }
            return keycloakJwtDecoder.decode(token);
        };
    }
}
