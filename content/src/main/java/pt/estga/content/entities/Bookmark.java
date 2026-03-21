package pt.estga.content.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import pt.estga.content.enums.TargetType;
import pt.estga.user.entities.User;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Bookmark {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @CreationTimestamp
    private Instant createdAt;
}

