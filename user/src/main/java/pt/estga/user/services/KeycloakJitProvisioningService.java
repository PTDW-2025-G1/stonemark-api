package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class KeycloakJitProvisioningService {

    private final UserService userService;
    private KeycloakJitProvisioningService self;
    private static final Logger log = LoggerFactory.getLogger(KeycloakJitProvisioningService.class);
    private static final int SUB_SUFFIX_LENGTH = 8;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final Pattern ALLOWED_USERNAME = Pattern.compile("[^a-zA-Z0-9._-]");

    @Transactional
    @Cacheable(value = "user_provisioning", key = "#snapshot.sub()")
    public User resolveOrProvision(KeycloakIdentitySnapshot snapshot) {
        // Require Keycloak subject as the primary external lookup key
        if (snapshot.sub() == null || snapshot.sub().isBlank()) {
            log.warn("Keycloak snapshot missing sub claim; cannot resolve or provision user: {}", snapshot);
            throw new IllegalArgumentException("Keycloak snapshot must contain a subject (sub)");
        }

        return userService.findByKeycloakSub(snapshot.sub())
                .map(existing -> syncSnapshot(existing, snapshot))
                .orElseGet(() -> linkOrCreate(snapshot));
    }

    private User linkOrCreate(KeycloakIdentitySnapshot snapshot) {
        if (snapshot.email() != null && snapshot.emailVerified()) {
            return userService.findByEmail(snapshot.email())
                    .map(existing -> linkExistingUser(existing, snapshot))
                    .orElseGet(() -> createUser(snapshot));
        }

        return createUser(snapshot);
    }

    private User linkExistingUser(User existing, KeycloakIdentitySnapshot snapshot) {
        // Only link when the identity provider has asserted the email as verified
        if (!snapshot.emailVerified()) {
            throw new IllegalStateException("Cannot link account: email address is not verified by Keycloak");
        }

        if (existing.getKeycloakSub() != null && !existing.getKeycloakSub().equals(snapshot.sub())) {
            throw new IllegalStateException("User already linked to another Keycloak subject");
        }

        // Link external identity and persist only if something actually changed
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
        return userService.create(user);
    }

    private User syncSnapshot(User user, KeycloakIdentitySnapshot snapshot) {
        boolean changed = false;

        // Email and verification status: update only when different
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

        // Keycloak subject: ensure local DB stores the external lookup key; persist when changed
        if (snapshot.sub() != null && !snapshot.sub().equals(user.getKeycloakSub())) {
            user.setKeycloakSub(snapshot.sub());
            changed = true;
        }

        // Do not overwrite local firstName/lastName to maintain local data ownership

        // Update last login timestamp only when it's stale to avoid frequent DB writes
        Instant now = Instant.now();
        long minutesSinceLast = user.getLastLoginAt() == null ? Long.MAX_VALUE : Duration.between(user.getLastLoginAt(), now).toMinutes();
        if (user.getLastLoginAt() == null || minutesSinceLast >= 5) {
            user.setLastLoginAt(now);
            changed = true;
        }

        if (changed) {
            User updated = userService.update(user);
            // Evict cache via the proxied self reference so @CacheEvict takes effect
            if (self != null) {
                self.evictProvisioningCache(updated.getKeycloakSub());
            }
            return updated;
        }

        return user;
    }

    @CacheEvict(value = "user_provisioning", key = "#key")
    // Separate method so Spring can apply the cache eviction advice
    @SuppressWarnings("unused")
    public void evictProvisioningCache(String key) {
        // Intentionally left blank; annotation triggers eviction via Spring AOP.
    }

    @Autowired
    public void setSelf(KeycloakJitProvisioningService self) {
        this.self = self;
    }

    private String resolveUsername(KeycloakIdentitySnapshot snapshot) {
        // 1. Pick readable base
        String base = snapshot.preferredUsername();
        if (base == null || base.isBlank()) {
            base = (snapshot.email() != null && snapshot.email().contains("@"))
                    ? snapshot.email().split("@")[0]
                    : "user";
        }

        // 2. Sanitize base to remove emojis and special characters
        base = sanitizeUsernameBase(base);

        // 3. Deterministic suffix from Keycloak sub (keep only alphanumeric characters)
        String sub = snapshot.sub() != null ? snapshot.sub() : UUID.randomUUID().toString();
        String suffix = alphaNumericSuffixFromSub(sub);

        String candidate = base + "_" + suffix;

        // Ensure max length
        if (candidate.length() > MAX_USERNAME_LENGTH) {
            int keep = MAX_USERNAME_LENGTH - 1 - suffix.length(); // -1 for '_'
            String truncatedBase = base.substring(0, Math.min(base.length(), keep));
            candidate = truncatedBase + "_" + suffix;
        }

        return candidate.toLowerCase();
    }

    private String sanitizeUsernameBase(String input) {
        if (input == null) return "user";

        // Normalize unicode to separate diacritics, then remove them
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);

        // Remove characters not allowed in usernames (this also strips emojis)
        String cleaned = ALLOWED_USERNAME.matcher(normalized).replaceAll("");

        // Collapse consecutive dots/underscores/dashes
        cleaned = cleaned.replaceAll("[._-]{2,}", "_");

        // Trim and enforce minimum content
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return "user";

        // Truncate to reasonable length (leave space for suffix)
        int maxBase = Math.max(1, MAX_USERNAME_LENGTH - 1 - SUB_SUFFIX_LENGTH);
        if (cleaned.length() > maxBase) {
            cleaned = cleaned.substring(0, maxBase);
        }

        return cleaned;
    }

    private String alphaNumericSuffixFromSub(String sub) {
        // Assume sanitized Keycloak sub yields at least one alphanumeric character
        String subAlpha = sub.replaceAll("[^A-Za-z0-9]", "");
        return subAlpha.substring(0, Math.min(subAlpha.length(), SUB_SUFFIX_LENGTH));
    }
}
