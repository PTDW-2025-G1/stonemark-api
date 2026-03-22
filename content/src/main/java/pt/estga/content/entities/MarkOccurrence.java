package pt.estga.content.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.audit.AuditedEntity;
import pt.estga.shared.utils.PgVectorType;
import pt.estga.user.entities.User;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkOccurrence extends AuditedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private MediaFile cover;

    @Type(PgVectorType.class)
    @Column(columnDefinition = "vector")
    private float[] embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    private Mark mark;

    @ManyToOne(fetch = FetchType.LAZY)
    private Monument monument;

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    private Instant publishedAt;

    @Builder.Default
    private Boolean active = true;

    @PrePersist
    public void prePersist() {
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
    }

}
