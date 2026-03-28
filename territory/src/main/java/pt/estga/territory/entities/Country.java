package pt.estga.territory.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import lombok.*;
import pt.estga.shared.entities.BaseEntity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Country extends BaseEntity {

    @Id
    @GeneratedValue
    private Integer id;

    private String code;

    private String name;
}
