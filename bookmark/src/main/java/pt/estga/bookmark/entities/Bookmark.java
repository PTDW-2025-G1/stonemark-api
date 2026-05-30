package pt.estga.bookmark.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import pt.estga.bookmark.enums.BookmarkTargetType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookmark")
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

    @Column(nullable = false)
    private Long createdById;

    @CreationTimestamp
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookmarkTargetType targetType;

    @Column(nullable = false)
    private String targetId;
}
