package pt.estga.territory.entities;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import pt.estga.shared.entities.BaseEntity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AdministrativeDivision extends BaseEntity {

    @Id
    private Long id;

    private Integer osmAdminLevel;

    private String name;

    @Column(columnDefinition = "geometry")
    private Geometry geometry;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision parent;

    @Deprecated
    private String countryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    private Country country;

    private int monumentsCount = 0;

}
