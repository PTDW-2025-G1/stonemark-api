package pt.estga.user.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import pt.estga.shared.entities.BaseEntity;
import pt.estga.shared.enums.UserRole;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "_user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    private String firstName;
    private String lastName;
    private String username;

    @Column(unique = true)
    private String email;

    @Builder.Default
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "password_hash")
    private String passwordHash;

    @Builder.Default
    @Column(name = "token_version")
    private int tokenVersion = 0;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    private boolean accountLocked;
    @Builder.Default
    private boolean enabled = false;

    private Instant lastLoginAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
