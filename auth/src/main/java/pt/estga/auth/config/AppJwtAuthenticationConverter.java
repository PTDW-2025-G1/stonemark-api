package pt.estga.auth.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pt.estga.shared.core.models.AppPrincipal;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Component
public class AppJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = jwt.getClaim("user_id");
        String username = jwt.getClaim("preferred_username");

        Collection<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);

        AppPrincipal principal = AppPrincipal.builder()
                .id(userId)
                .identifier(username)
                .password(null)
                .authorities(Collections.unmodifiableCollection(authorities))
                .enabled(true)
                .accountNonLocked(true)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private static Collection<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        String role = jwt.getClaim("role");
        if (role == null || role.isBlank()) {
            return Set.of();
        }
        return Set.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
