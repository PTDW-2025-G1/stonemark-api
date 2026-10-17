package pt.estga.bookmark.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import pt.estga.bookmark.enums.BookmarkTargetType;

import java.time.Instant;
import java.util.UUID;

/**
 * A resource saved by a user. The target is referenced by type and id; the
 * unique constraint keeps one bookmark per user, per target.
 */
@Entity
@Table(
        name = "bookmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bookmark_user_target",
                columnNames = {"user_id", "target_type", "target_id"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Bookmark {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private BookmarkTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;
}
