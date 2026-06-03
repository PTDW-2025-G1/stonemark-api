package pt.estga.mark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.shared.converters.ValidationStateConverter;
import pt.estga.shared.entities.BaseEntity;
import pt.estga.shared.enums.ValidationState;

import java.util.List;

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

    @OneToMany(mappedBy = "mark")
    private List<MarkOccurrence> occurrences;

    @Builder.Default
    @Convert(converter = ValidationStateConverter.class)
    @Column(nullable = false)
    private ValidationState validationState = ValidationState.PROVISIONAL;

}
