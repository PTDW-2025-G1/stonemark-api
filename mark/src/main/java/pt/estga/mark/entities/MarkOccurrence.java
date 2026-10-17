package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.shared.jpa.converters.ValidationStateConverter;
import pt.estga.shared.core.enums.ValidationState;
import pt.estga.shared.jpa.entities.BaseEntity;

import java.util.List;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"mark_id", "monument_id"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkOccurrence extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Mark mark;

    @Column(nullable = true)
    private Long monumentId;

    @OneToMany(mappedBy = "occurrence", cascade = CascadeType.MERGE)
    private List<MarkEvidence> evidences;

    @Builder.Default
    @Convert(converter = ValidationStateConverter.class)
    @Column(nullable = false)
    private ValidationState validationState = ValidationState.PROVISIONAL;

}
