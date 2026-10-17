package pt.estga.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Security configuration for the password/JWT authentication model.
 *
 * <p>Public routes stay open (anonymous submissions); everything else requires an
 * authenticated principal, and admin/moderator endpoints additionally enforce roles
 * via {@code @PreAuthorize} (enabled here).
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${application.security.disabled:false}")
    private boolean securityDisabled;

    private static final String[] OPEN_API_ROUTES = {
            "/",
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html"
    };
    private static final String PUBLIC_ROUTE = "/api/v1/public/**";
    private static final String AUTH_ROUTE = "/api/v1/auth/**";
    private static final String MEDIA_UPLOAD_ROUTE = "/api/v1/public/media";
    private static final String MEDIA_WRITE_ROUTE = "/api/v1/public/media/**";
    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:*",
            "https://stonemark.pt",
            "https://*.stonemark.pt"
    };

    private final AppJwtAuthenticationConverter appJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of(ALLOWED_ORIGINS));
                    config.setAllowedMethods(List.of("*"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(OPEN_API_ROUTES).permitAll();
                    auth.requestMatchers("/actuator/**").permitAll();

                    // Media writes are not public: uploaded evidence is stored through
                    // the submission flow, not by anonymous clients.
                    auth.requestMatchers(HttpMethod.POST, MEDIA_UPLOAD_ROUTE).authenticated();
                    auth.requestMatchers(HttpMethod.DELETE, MEDIA_WRITE_ROUTE).authenticated();

                    auth.requestMatchers(PUBLIC_ROUTE).permitAll();
                    auth.requestMatchers(AUTH_ROUTE).permitAll();

                    if (securityDisabled) {
                        auth.anyRequest().permitAll();
                    } else {
                        auth.anyRequest().authenticated();
                    }
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(appJwtAuthenticationConverter)
                        )
                );

        return http.build();
    }
}
