package pt.estga.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.auth.config.JwtService;
import pt.estga.auth.dto.AuthResponse;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Authenticates a user with a username (or email) and password, and issues a
     * new access/refresh token pair.
     */
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
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getRole().name(),
                user.getId()
        );
    }
}
