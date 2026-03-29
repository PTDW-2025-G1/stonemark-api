package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.file.entities.MediaFile;
import pt.estga.monument.Monument;
import pt.estga.shared.entities.BaseEntity;
import pt.estga.user.entities.User;

import java.time.Instant;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkOccurrence extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(columnDefinition = "vector")
    private float[] embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    private Mark mark;

    @ManyToOne(fetch = FetchType.LAZY)
    private Monument monument;

    @OneToMany(mappedBy = "occurrence", cascade = CascadeType.ALL)
    private List<MarkEvidence> evidences;

}
