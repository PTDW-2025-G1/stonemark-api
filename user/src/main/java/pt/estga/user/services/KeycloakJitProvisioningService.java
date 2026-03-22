package pt.estga.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.dtos.KeycloakIdentitySnapshot;
import pt.estga.user.entities.User;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KeycloakJitProvisioningService {

    private final UserService userService;

    @Transactional
    public User resolveOrProvision(KeycloakIdentitySnapshot snapshot) {
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
        if (existing.getKeycloakSub() != null && !existing.getKeycloakSub().equals(snapshot.sub())) {
            throw new IllegalStateException("User already linked to another Keycloak subject");
        }

        existing.setKeycloakSub(snapshot.sub());
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

        return userService.create(user);
    }

    private User syncSnapshot(User user, KeycloakIdentitySnapshot snapshot) {
        if (snapshot.preferredUsername() != null && !snapshot.preferredUsername().isBlank()) {
            user.setUsername(snapshot.preferredUsername());
        }

        if (snapshot.givenName() != null && !snapshot.givenName().isBlank()) {
            user.setFirstName(snapshot.givenName());
        }

        if (snapshot.familyName() != null && !snapshot.familyName().isBlank()) {
            user.setLastName(snapshot.familyName());
        }

        if (snapshot.email() != null) {
            user.setEmail(snapshot.email());
            user.setEmailVerified(snapshot.emailVerified());
        }

        return userService.update(user);
    }

    private String resolveUsername(KeycloakIdentitySnapshot snapshot) {
        // 1. Pick the best starting point
        String base = snapshot.preferredUsername();
        if (base == null || base.isBlank()) {
            base = (snapshot.email() != null && snapshot.email().contains("@"))
                    ? snapshot.email().split("@")[0]
                    : "user";
        }

        // 2. Sanitize: Remove non-alphanumeric characters (keep it clean)
        base = base.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        // 3. Extract deterministic suffix from sub
        // Using 8 chars gives 16^8 (~4.2 billion) possibilities, safe for collisions
        String sub = snapshot.sub() != null ? snapshot.sub() : UUID.randomUUID().toString();
        String suffix = sub.substring(0, Math.min(sub.length(), 8));

        return base + "_" + suffix;
    }
}
