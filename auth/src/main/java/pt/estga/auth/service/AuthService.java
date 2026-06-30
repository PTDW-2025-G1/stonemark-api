package pt.estga.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.auth.config.JwtService;
import pt.estga.auth.dto.AuthResponse;
import pt.estga.auth.oauth.GoogleTokenVerifier;
import pt.estga.user.entities.Role;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.RoleRepository;
import pt.estga.user.repositories.UserRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Transactional
    public AuthResponse authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getPasswordHash() == null) {
            throw new IllegalArgumentException("No password set for this account");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        if (user.isAccountLocked()) {
            throw new IllegalArgumentException("Account is locked");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse authenticateWithGoogle(String googleIdToken) {
        Map<String, Object> payload = googleTokenVerifier.verify(googleIdToken);

        String googleSub = (String) payload.get("sub");
        String email = (String) payload.get("email");
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");
        Boolean emailVerified = (Boolean) payload.get("email_verified");

        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("Google subject claim is missing");
        }

        User user = userRepository.findByGoogleSub(googleSub).orElse(null);

        if (user != null) {
            log.debug("Found existing user id={} by Google sub", user.getId());
            return buildAuthResponse(user);
        }

        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                if (user.getGoogleSub() != null && !user.getGoogleSub().equals(googleSub)) {
                    log.warn("Email {} already linked to different Google identity", email);
                    throw new IllegalArgumentException("Account conflict: email linked to different Google identity");
                }
                user.setGoogleSub(googleSub);
                if (givenName != null) user.setFirstName(givenName);
                if (familyName != null) user.setLastName(familyName);
                userRepository.save(user);
                log.info("Linked existing user id={} to Google sub={}", user.getId(), googleSub);
                return buildAuthResponse(user);
            }
        }

        user = User.builder()
                .username(resolveUsername(email, givenName, familyName, googleSub))
                .firstName(givenName)
                .lastName(familyName)
                .email(email)
                .googleSub(googleSub)
                .emailVerified(emailVerified != null && emailVerified)
                .enabled(true)
                .roles(new HashSet<>(Set.of(getDefaultUserRole())))
                .build();

        user = userRepository.save(user);
        log.info("Created new user id={} for Google sub={}", user.getId(), googleSub);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshTokenValue) {
        Jwt jwt = jwtService.validateAndParse(refreshTokenValue);

        if (!"refresh".equals(jwt.getClaim("type"))) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        Long userId = Long.parseLong(jwt.getSubject());
        Integer tokenVersion = jwt.getClaim("token_version");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getTokenVersion() != (tokenVersion != null ? tokenVersion : 0)) {
            throw new IllegalArgumentException("Token has been revoked");
        }

        if (!user.isEnabled() || user.isAccountLocked()) {
            throw new IllegalArgumentException("Account is disabled or locked");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        User userWithRoles = userRepository.findByIdWithRoles(user.getId())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        Set<String> roles = userWithRoles.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String highestRole = roles.stream()
                .filter(name -> name.equals("ADMIN") || name.equals("MODERATOR") || name.equals("USER"))
                .findFirst()
                .orElse("USER");

        return new AuthResponse(
                jwtService.generateAccessToken(userWithRoles, roles),
                jwtService.generateRefreshToken(userWithRoles),
                highestRole,
                userWithRoles.getId()
        );
    }

    private Role getDefaultUserRole() {
        return roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));
    }

    private String resolveUsername(String email, String givenName, String familyName, String googleSub) {
        if (givenName != null && familyName != null) {
            String candidate = (givenName + "." + familyName).toLowerCase().replaceAll("[^a-z0-9._-]", "");
            if (!userRepository.existsByUsername(candidate)) return candidate;
        }
        if (email != null && email.contains("@")) {
            String candidate = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9._-]", "");
            if (!candidate.isEmpty() && !userRepository.existsByUsername(candidate)) return candidate;
        }
        String suffix = googleSub.replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(googleSub.length(), 8));
        return "user_" + suffix.toLowerCase();
    }
}
