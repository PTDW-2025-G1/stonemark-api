package pt.estga.boot.config;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserService userService;
    private final AppPrincipalFactory principalFactory;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Bean
    public UserDetailsService userDetailsService() {
        return loginIdentifier -> {
            User user = userService.findByUsername(loginIdentifier)
                    .or(() -> userService.findByEmail(loginIdentifier))
                    .or(() -> userService.findByPhone(loginIdentifier))
                    .orElseThrow(() -> new UsernameNotFoundException(loginIdentifier));

            return principalFactory.fromLoginUser(user);
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider
                = new DaoAuthenticationProvider(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PhoneNumberUtil phoneNumberUtil() {
        return PhoneNumberUtil.getInstance();
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
