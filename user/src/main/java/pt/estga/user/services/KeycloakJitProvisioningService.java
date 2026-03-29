package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.regex.Pattern;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.dtos.KeycloakIdentitySnapshot;
import pt.estga.user.entities.User;

import java.util.UUID;
import java.time.Instant;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KeycloakJitProvisioningService {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private KeycloakJitProvisioningService self;
    private static final Logger log = LoggerFactory.getLogger(KeycloakJitProvisioningService.class);
    private static final int SUB_SUFFIX_LENGTH = 8;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final Pattern ALLOWED_USERNAME = Pattern.compile("[^a-zA-Z0-9._-]");

    /**
     * Setter for self-injection to allow proxy-based cache eviction.
     * @Lazy is critical here to prevent circular dependency on startup.
     */
    @Autowired
    public void setSelf(@Lazy KeycloakJitProvisioningService self) {
        this.self = self;
    }

    @Transactional
    @Cacheable(value = "user_provisioning", key = "#snapshot.sub()")
    public User resolveOrProvision(KeycloakIdentitySnapshot snapshot) {
        if (snapshot.sub() == null || snapshot.sub().isBlank()) {
            log.warn("Keycloak snapshot missing sub claim; cannot resolve or provision user: {}", snapshot);
            throw new IllegalArgumentException("Keycloak snapshot must contain a subject (sub)");
        }

        return userQueryService.findByKeycloakSub(snapshot.sub())
                .map(existing -> syncSnapshot(existing, snapshot))
                .orElseGet(() -> linkOrCreate(snapshot));
    }

    private User linkOrCreate(KeycloakIdentitySnapshot snapshot) {
        if (snapshot.email() != null && snapshot.emailVerified()) {
            return userQueryService.findByEmail(snapshot.email())
                    .map(existing -> linkExistingUser(existing, snapshot))
                    .orElseGet(() -> createUser(snapshot));
        }
        return createUser(snapshot);
    }

    private User linkExistingUser(User existing, KeycloakIdentitySnapshot snapshot) {
        if (!snapshot.emailVerified()) {
            throw new IllegalStateException("Cannot link account: email address is not verified by Keycloak");
        }

        if (existing.getKeycloakSub() != null && !existing.getKeycloakSub().equals(snapshot.sub())) {
            throw new IllegalStateException("User already linked to another Keycloak subject");
        }

        existing.setKeycloakSub(snapshot.sub());
        log.info("Linking existing user id={} email={} to Keycloak sub={}", existing.getId(), existing.getEmail(), snapshot.sub());

        return syncSnapshot(existing, snapshot);
    }

    private User createUser(KeycloakIdentitySnapshot snapshot) {
        User user = User.builder()
                .username(resolveUsername(snapshot))
                .firstName(snapshot.givenName())
                .lastName(snapshot.familyName())
                .email(snapshot.email())
                .emailVerified(snapshot.emailVerified())
                .keycloakSub(snapshot.sub())
                .enabled(true)
                .role(UserRole.USER)
                .build();

        log.info("Creating new user username={} email={} keycloakSub={}", user.getUsername(), user.getEmail(), user.getKeycloakSub());
        return userCommandService.create(user);
    }

    private User syncSnapshot(User user, KeycloakIdentitySnapshot snapshot) {
        boolean changed = false;

        if (snapshot.email() != null) {
            if (!snapshot.email().equals(user.getEmail())) {
                user.setEmail(snapshot.email());
                changed = true;
            }
            if (snapshot.emailVerified() != user.isEmailVerified()) {
                user.setEmailVerified(snapshot.emailVerified());
                changed = true;
            }
        }

        if (snapshot.sub() != null && !snapshot.sub().equals(user.getKeycloakSub())) {
            user.setKeycloakSub(snapshot.sub());
            changed = true;
        }

        Instant now = Instant.now();
        long minutesSinceLast = user.getLastLoginAt() == null ? Long.MAX_VALUE : Duration.between(user.getLastLoginAt(), now).toMinutes();
        if (user.getLastLoginAt() == null || minutesSinceLast >= 5) {
            user.setLastLoginAt(now);
            changed = true;
        }

        if (changed) {
            User updated = userCommandService.update(user);
            // Use 'self' proxy to trigger @CacheEvict
            if (self != null) {
                self.evictProvisioningCache(updated.getKeycloakSub());
            }
            return updated;
        }

        return user;
    }

    @CacheEvict(value = "user_provisioning", key = "#key")
    public void evictProvisioningCache(String key) {
        log.trace("Evicting user_provisioning cache for key: {}", key);
    }

    // --- Helper Methods (Username Logic) ---

    private String resolveUsername(KeycloakIdentitySnapshot snapshot) {
        String base = snapshot.preferredUsername();
        if (base == null || base.isBlank()) {
            base = (snapshot.email() != null && snapshot.email().contains("@"))
                    ? snapshot.email().split("@")[0]
                    : "user";
        }

        base = sanitizeUsernameBase(base);
        String sub = snapshot.sub() != null ? snapshot.sub() : UUID.randomUUID().toString();
        String suffix = alphaNumericSuffixFromSub(sub);

        String candidate = base + "_" + suffix;

        if (candidate.length() > MAX_USERNAME_LENGTH) {
            int keep = MAX_USERNAME_LENGTH - 1 - suffix.length();
            String truncatedBase = base.substring(0, Math.min(base.length(), keep));
            candidate = truncatedBase + "_" + suffix;
        }

        return candidate.toLowerCase();
    }

    private String sanitizeUsernameBase(String input) {
        if (input == null) return "user";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
        String cleaned = ALLOWED_USERNAME.matcher(normalized).replaceAll("");
        cleaned = cleaned.replaceAll("[._-]{2,}", "_");
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return "user";

        int maxBase = Math.max(1, MAX_USERNAME_LENGTH - 1 - SUB_SUFFIX_LENGTH);
        if (cleaned.length() > maxBase) {
            cleaned = cleaned.substring(0, maxBase);
        }
        return cleaned;
    }

    private String alphaNumericSuffixFromSub(String sub) {
        String subAlpha = sub.replaceAll("[^A-Za-z0-9]", "");
        return subAlpha.substring(0, Math.min(subAlpha.length(), SUB_SUFFIX_LENGTH));
    }
}
