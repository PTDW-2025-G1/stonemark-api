package pt.estga.monument;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.shared.entities.AuditedEntity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Monument extends AuditedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String externalId;

    private String name;
    private String protectionTitle;
    private String description;
    private Double latitude;
    private Double longitude;
    private String website;

    private String street;
    private String houseNumber;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @ManyToOne(fetch = FetchType.EAGER)
    private AdministrativeDivision parish;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision municipality;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision district;

    @Builder.Default
    private Boolean active = true;

    @PrePersist
    @PreUpdate
    public void updateLocation() {
        if (latitude != null && longitude != null) {
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
            this.location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }
    }

}
