package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.audit.AuditedEntity;

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

    @Column(columnDefinition = "vector")
    private float[] embedding;

    @Builder.Default
    private Boolean active = true;

}
