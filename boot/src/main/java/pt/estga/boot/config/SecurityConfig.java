package pt.estga.boot.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import pt.estga.auth.services.LogoutService;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${telegram.bot.webhook-path}")
    private String telegramWebhookPath;

    @Value("${whatsapp.bot.webhook-path}")
    private String whatsappWebhookPath;

    @Value("${chatbot.auth.optional:true}")
    private boolean chatbotAuthOptional;

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
    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:*",
            "https://stonemark.pt",
            "https://*.stonemark.pt"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutService logoutService;

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
                    auth.requestMatchers(PUBLIC_ROUTE).permitAll();
                    auth.requestMatchers(AUTH_ROUTE).permitAll();

                    if (chatbotAuthOptional) {
                        auth.requestMatchers(telegramWebhookPath, whatsappWebhookPath).permitAll();
                    } else {
                        auth.requestMatchers(telegramWebhookPath, whatsappWebhookPath).authenticated();
                    }

                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(logoutService)
                        .logoutSuccessHandler(
                                (
                                        request,
                                        response,
                                        authentication
                                ) -> response.setStatus(HttpServletResponse.SC_OK)
                        )
                );

        return http.build();
    }
}
