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

    @Column(unique = true)
    private String phone;

    @Column(name = "keycloak_sub", unique = true)
    private String keycloakSub;

    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean phoneVerified = false;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private MediaFile photo;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private boolean accountLocked;
    @Builder.Default
    private boolean enabled = false;

    @CreationTimestamp
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return accountLocked == user.accountLocked
                && enabled == user.enabled
                && emailVerified == user.emailVerified
                && phoneVerified == user.phoneVerified
                && Objects.equals(id, user.id)
                && Objects.equals(firstName, user.firstName)
                && Objects.equals(lastName, user.lastName)
                && Objects.equals(username, user.username)
                && Objects.equals(email, user.email)
                && Objects.equals(phone, user.phone)
                && Objects.equals(keycloakSub, user.keycloakSub)
                && role == user.role
                && Objects.equals(createdAt, user.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, username, email, phone, keycloakSub, emailVerified, phoneVerified, role, accountLocked, enabled, createdAt);
    }
}
