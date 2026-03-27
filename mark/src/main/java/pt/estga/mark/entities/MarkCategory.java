package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkCategory {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private MarkCategory parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    private List<MarkCategory> subCategories;

}
