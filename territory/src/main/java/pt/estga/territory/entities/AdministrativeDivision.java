package pt.estga.territory.entities;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import pt.estga.commoninfra.entities.BaseEntity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AdministrativeDivision extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Column(columnDefinition = "geometry")
    private Geometry geometry;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision parent;

    @ManyToOne(fetch = FetchType.LAZY)
    private Country country;

}
