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
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

import java.util.List;

/**
 * Security configuration for the password/JWT authentication model.
 *
 * <p>Public routes stay open (anonymous submissions); everything else requires an
 * authenticated principal, and admin/moderator endpoints additionally enforce roles
 * via {@code @PreAuthorize} (enabled here). Roles are ordered
 * ({@code ADMIN > MODERATOR > REVIEWER > USER}), so an endpoint only declares the
 * minimum role it needs.
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

    /**
     * Ordered roles, highest first: an ADMIN satisfies a MODERATOR requirement, and
     * so on down to USER.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MODERATOR
                ROLE_MODERATOR > ROLE_REVIEWER
                ROLE_REVIEWER > ROLE_USER
                """);
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

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
