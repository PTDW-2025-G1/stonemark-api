package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.file.entities.MediaFile;
import pt.estga.shared.entities.AuditedEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private String name;

    @Column
    private String description;

    @OneToOne(cascade = CascadeType.ALL)
    private MediaFile referenceImage;

    @Column(columnDefinition = "vector")
    private float[] canonicalEmbedding;

    @ManyToMany
    @JoinTable(
            name = "mark_category_mapping",
            joinColumns = @JoinColumn(name = "mark_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<MarkCategory> categories = new HashSet<>();

    @OneToMany(mappedBy = "mark")
    private List<MarkOccurrence> occurrences;

    @Builder.Default
    private Boolean active = true;
}
