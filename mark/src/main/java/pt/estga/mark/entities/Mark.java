package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.audit.AuditedEntity;
import pt.estga.shared.utils.PgVectorType;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Mark extends AuditedEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String description;

    @OneToOne
    private MediaFile cover;

    @Type(PgVectorType.class)
    @Column(columnDefinition = "vector")
    private float[] embedding;

    @Builder.Default
    private Boolean active = true;

}
