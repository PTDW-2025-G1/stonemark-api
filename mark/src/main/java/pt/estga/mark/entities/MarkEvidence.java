package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.converters.VectorConverter;
import pt.estga.shared.entities.BaseEntity;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidence extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL)
    private MediaFile file;

    @Convert(converter = VectorConverter.class)
    private float[] embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    private MarkOccurrence occurrence;
}