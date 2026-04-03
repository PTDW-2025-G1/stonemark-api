package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.shared.entities.BaseEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Mark extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    private String description;

    @ManyToMany
    @JoinTable(
            name = "mark_category_mapping",
            joinColumns = @JoinColumn(name = "mark_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<MarkCategory> categories = new HashSet<>();

    @OneToMany(mappedBy = "mark")
    private List<MarkOccurrence> occurrences;

}
