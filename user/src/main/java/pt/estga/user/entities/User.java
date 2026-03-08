package pt.estga.user.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.enums.UserRole;
import pt.estga.user.enums.TfaMethod;

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
    private String password;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TfaMethod tfaMethod = TfaMethod.NONE;
    private String tfaSecret;

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
                && Objects.equals(password, user.password)
                && Objects.equals(email, user.email)
                && Objects.equals(phone, user.phone)
                && role == user.role
                && tfaMethod == user.tfaMethod
                && Objects.equals(tfaSecret, user.tfaSecret)
                && Objects.equals(createdAt, user.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, username, password, email, phone, emailVerified, phoneVerified, role, accountLocked, enabled, tfaMethod, tfaSecret, createdAt);
    }
}
