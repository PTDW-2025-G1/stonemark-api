package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.models.AppPrincipal;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "user-permissions";

    @Transactional(readOnly = true)
    public AppPrincipal loadUserById(Long userId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            AppPrincipal cached = cache.get(userId, AppPrincipal.class);
            if (cached != null) {
                return cached;
            }
        }

        AppPrincipal principal = fetchPrincipal(userId);
        if (cache != null) {
            cache.put(userId, principal);
        }
        return principal;
    }

    public void invalidateCache(Long userId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    private AppPrincipal fetchPrincipal(Long userId) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toUnmodifiableSet());

        return AppPrincipal.builder()
                .id(user.getId())
                .identifier(user.getUsername())
                .password(null)
                .authorities(authorities)
                .enabled(user.isEnabled())
                .accountNonLocked(!user.isAccountLocked())
                .build();
    }
}
