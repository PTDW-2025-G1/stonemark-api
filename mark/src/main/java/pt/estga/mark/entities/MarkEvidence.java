package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.entities.AuditedEntity;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidence extends AuditedEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL)
    private MediaFile file;

    @Column(columnDefinition = "vector")
    private float[] embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    private MarkOccurrence occurrence;
}