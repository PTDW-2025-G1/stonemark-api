package pt.estga.monument.entities;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import pt.estga.commoninfra.entities.BaseEntity;
import pt.estga.territory.entities.AdministrativeDivision;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Monument extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String protectionTitle;
    private String description;
    private String website;

    private String address;
    private String postalCode;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision division;

}
