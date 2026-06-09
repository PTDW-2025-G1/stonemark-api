package pt.estga.boot.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.user.entities.Role;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.RoleRepository;
import pt.estga.user.repositories.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User googleUser = new DefaultOAuth2UserService().loadUser(userRequest);

        String googleSub = googleUser.getName();
        String email = googleUser.getAttribute("email");
        String givenName = googleUser.getAttribute("given_name");
        String familyName = googleUser.getAttribute("family_name");

        if (googleSub == null || googleSub.isBlank()) {
            throw new OAuth2AuthenticationException("Google subject claim is missing");
        }

        User user = userRepository.findByGoogleSub(googleSub).orElse(null);

        if (user != null) {
            return mapToPrincipal(user, googleUser.getAttributes());
        }

        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                if (user.getGoogleSub() != null && !user.getGoogleSub().equals(googleSub)) {
                    log.warn("Email {} already linked to different Google identity. Manual resolution required.", email);
                    throw new OAuth2AuthenticationException(
                            "Account conflict: email is linked to a different Google identity.");
                }

                user.setGoogleSub(googleSub);
                if (givenName != null) {
                    user.setFirstName(givenName);
                }
                if (familyName != null) {
                    user.setLastName(familyName);
                }
                userRepository.save(user);
                log.info("Linked existing user id={} to Google sub={}", user.getId(), googleSub);
                return mapToPrincipal(user, googleUser.getAttributes());
            }
        }

        user = User.builder()
                .username(resolveUsername(email, givenName, familyName, googleSub))
                .firstName(givenName)
                .lastName(familyName)
                .email(email)
                .googleSub(googleSub)
                .emailVerified(email != null)
                .enabled(true)
                .roles(new HashSet<>(Set.of(getDefaultUserRole())))
                .build();

        user = userRepository.save(user);
        log.info("Created new user id={} for Google sub={}", user.getId(), googleSub);
        return mapToPrincipal(user, googleUser.getAttributes());
    }

    private static OAuth2User mapToPrincipal(User user, Map<String, Object> attributes) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();

        return new DefaultOAuth2User(authorities, attributes, user.getUsername());
    }

    private Role getDefaultUserRole() {
        return roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found in database"));
    }

    private String resolveUsername(String email, String givenName, String familyName, String googleSub) {
        if (givenName != null && familyName != null) {
            String candidate = (givenName + "." + familyName).toLowerCase().replaceAll("[^a-z0-9._-]", "");
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        if (email != null && email.contains("@")) {
            String candidate = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9._-]", "");
            if (!candidate.isEmpty() && !userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        String suffix = googleSub.replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(googleSub.length(), 8));
        return "user_" + suffix.toLowerCase();
    }
}
