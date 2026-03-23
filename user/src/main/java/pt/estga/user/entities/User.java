package pt.estga.user.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.enums.UserRole;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "_user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String firstName;
    private String lastName;
    private String username;

    @Column(unique = true)
    private String email;

    /**
     * External Keycloak subject identifier (subclaim).
     * <p>
     * This value is used only as an external lookup key for JIT provisioning and
     * account linking. Internal domain relationships should reference the local
     * database id (Long) to preserve local data ownership and portability.
     */
    @Column(name = "keycloak_sub", unique = true)
    private String keycloakSub;

    /**
     * Snapshot of email verification status from Keycloak.
     * This field is synchronized during JIT (Just-In-Time) provisioning from the Keycloak JWT token.
     * Do NOT manually set this field - it is managed by Keycloak and synced on login.
     * Source: JWT claim 'email_verified'
     */
    @Builder.Default
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private boolean accountLocked;
    @Builder.Default
    private boolean enabled = false;

    @CreationTimestamp
    private Instant createdAt;

    private Instant lastLoginAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if (keycloakSub != null && user.keycloakSub != null) {
            return Objects.equals(keycloakSub, user.keycloakSub);
        }
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return keycloakSub != null ? Objects.hash(keycloakSub) : Objects.hash(id);
    }
}
