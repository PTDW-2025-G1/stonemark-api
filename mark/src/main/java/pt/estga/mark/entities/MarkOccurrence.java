package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.monument.Monument;
import pt.estga.shared.entities.BaseEntity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    private Monument monument;

    @OneToMany(mappedBy = "occurrence", cascade = CascadeType.MERGE)
    private List<MarkEvidence> evidences;

}
