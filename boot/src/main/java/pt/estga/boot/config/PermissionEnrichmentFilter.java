package pt.estga.boot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pt.estga.shared.models.AppPrincipal;
import pt.estga.user.services.AuthenticationService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PermissionEnrichmentFilter extends OncePerRequestFilter {

    private final AuthenticationService authenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof AppPrincipal principal
                && authentication.isAuthenticated()
                && principal.getAuthorities().isEmpty()) {

            Long userId = principal.getId();
            if (userId != null) {
                AppPrincipal enriched = authenticationService.loadUserById(userId);

                if (!enriched.isEnabled() || !enriched.isAccountNonLocked()) {
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is disabled or locked");
                    return;
                }

                var enrichedAuth = new UsernamePasswordAuthenticationToken(
                        enriched,
                        auth.getCredentials(),
                        enriched.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(enrichedAuth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
