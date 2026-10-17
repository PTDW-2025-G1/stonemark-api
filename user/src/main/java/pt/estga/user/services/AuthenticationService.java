package pt.estga.user.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.core.models.AppPrincipal;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AppPrincipal loadUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        var authorities = Set.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

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
