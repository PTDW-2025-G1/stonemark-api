package pt.estga.shared.models;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;

import java.util.Collection;

@Builder
public class AppPrincipal implements UserDetails, AuthenticatedPrincipal {

    private final Long id;
    @Getter
    private final String identifier; // username OR service name
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;

    @Override
    public Long getId() {
        return id;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public String getPassword() {
        return password;
    }

    @Override public String getUsername() {
        return identifier;
    }

    @Override public boolean isAccountNonExpired() {
        return true;
    }

    @Override public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override public boolean isEnabled() {
        return enabled;
    }

    @Override public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
}
