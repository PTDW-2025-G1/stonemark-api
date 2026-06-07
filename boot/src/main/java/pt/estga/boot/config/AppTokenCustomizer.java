package pt.estga.boot.config;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import pt.estga.sharedcore.models.AppPrincipal;

@Component
public class AppTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getPrincipal().getPrincipal() instanceof AppPrincipal app) {
            context.getClaims().claim("user_id", app.getId());
            context.getClaims().claim("preferred_username", app.getUsername());
        }
    }
}
