package pt.estga.boot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class ApplicationConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public JwtDecoder keycloakJwtDecoder() {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            return jwt -> {
                throw new IllegalStateException("Keycloak JWK Set URI not configured");
            };
        }
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
