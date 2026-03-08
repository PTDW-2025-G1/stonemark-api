package pt.estga.boot.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
            for (Object role : realmRoles) {
                if (role instanceof String roleName) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
                }
            }
        }

        // Extract resource/client roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Object clientRoles : resourceAccess.values()) {
                if (clientRoles instanceof Map<?, ?> clientRolesMap) {
                    Object roles = clientRolesMap.get("roles");
                    if (roles instanceof List<?> rolesList) {
                        for (Object role : rolesList) {
                            if (role instanceof String roleName) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
                            }
                        }
                    }
                }
            }
        }

        return authorities;
    }
}

