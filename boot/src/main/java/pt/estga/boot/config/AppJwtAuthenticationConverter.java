package pt.estga.boot.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pt.estga.shared.models.AppPrincipal;

import java.util.Collections;

@Component
public class AppJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = jwt.getClaim("user_id");
        String username = jwt.getClaim("preferred_username");

        AppPrincipal principal = AppPrincipal.builder()
                .id(userId)
                .identifier(username)
                .password(null)
                .authorities(Collections.emptyList())
                .enabled(true)
                .accountNonLocked(true)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }
}
