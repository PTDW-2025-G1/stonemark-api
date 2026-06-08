package pt.estga.boot.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pt.estga.commoncore.models.AppPrincipal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @SuppressWarnings("unchecked")
    private static Collection<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> permissions = jwt.getClaim("permissions");
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}
