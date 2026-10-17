package pt.estga.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pt.estga.user.entities.Role;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.RoleRepository;
import pt.estga.user.repositories.UserRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates an initial administrator from configuration so the moderation API is
 * reachable on a fresh deployment. Disabled unless a username and password are
 * configured; never overwrites an existing account.
 *
 * <p>Configure with {@code APPLICATION_BOOTSTRAP_ADMIN_USERNAME} and
 * {@code APPLICATION_BOOTSTRAP_ADMIN_PASSWORD} (email optional).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapSeeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.bootstrap.admin.username:}")
    private String username;

    @Value("${application.bootstrap.admin.password:}")
    private String password;

    @Value("${application.bootstrap.admin.email:}")
    private String email;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.debug("Admin bootstrap skipped: no credentials configured");
            return;
        }
        if (userRepository.existsByUsername(username)) {
            log.info("Admin bootstrap skipped: user '{}' already exists", username);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ADMIN")
                        .description("Bootstrapped admin role")
                        .build()));

        User admin = User.builder()
                .username(username)
                .email(StringUtils.hasText(email) ? email : null)
                .passwordHash(passwordEncoder.encode(password))
                .enabled(true)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();

        userRepository.save(admin);
        log.warn("Bootstrapped admin user '{}' from configuration", username);
    }
}
