package pt.estga.boot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pt.estga.shared.enums.PrincipalType;
import pt.estga.shared.models.AppPrincipal;
import pt.estga.user.dtos.KeycloakIdentitySnapshot;
import pt.estga.user.entities.User;
import pt.estga.user.services.KeycloakJitProvisioningService;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakJwtAuthoritiesConverter authoritiesConverter;
    private final KeycloakJitProvisioningService jitProvisioningService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

        // JIT provisioning: resolve or create user from JWT claims
        KeycloakIdentitySnapshot snapshot = KeycloakIdentitySnapshot.fromClaims(jwt.getClaims());
        User user = jitProvisioningService.resolveOrProvision(snapshot);

        // Build AppPrincipal from resolved user
        AppPrincipal principal = AppPrincipal.builder()
                .id(user.getId())
                .type(PrincipalType.USER)
                .identifier(user.getUsername())
                .password(null)
                .authorities(authorities)
                .enabled(user.isEnabled())
                .accountNonLocked(!user.isAccountLocked())
                .build();

        // Return authentication with AppPrincipal as the principal (not in details)
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}


