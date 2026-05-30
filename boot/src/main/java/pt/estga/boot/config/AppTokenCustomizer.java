package pt.estga.boot.config;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import pt.estga.shared.models.AppPrincipal;

import java.util.List;
import java.util.Map;

@Component
public class AppTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getPrincipal().getPrincipal() instanceof AppPrincipal app) {
            context.getClaims().claim("user_id", app.getId());
            context.getClaims().claim("preferred_username", app.getUsername());
            context.getClaims().claim("realm_access",
                    Map.of("roles", List.of(app.getAuthorities().stream()
                            .map(a -> a.getAuthority().replace("ROLE_", ""))
                            .toList())));
        }
    }
}
