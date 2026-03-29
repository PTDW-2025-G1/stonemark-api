package pt.estga.territory.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.shared.entities.BaseEntity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AdministrativeLevel extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private int osmLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    private Country country;

}
