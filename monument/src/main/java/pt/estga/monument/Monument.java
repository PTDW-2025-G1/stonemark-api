package pt.estga.monument;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import pt.estga.shared.entities.BaseEntity;
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

    @Column(unique = true)
    private String externalId;

    private String name;
    private String protectionTitle;
    private String description;
    // Todo: delete latitude and longitude
    private Double latitude;
    private Double longitude;
    private String website;

    private String street;
    private String houseNumber;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    // Todo: leave only 1 AD (parish -> division)
    @Deprecated
    @ManyToOne(fetch = FetchType.EAGER)
    private AdministrativeDivision parish;

    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision municipality;

    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision district;

    @PrePersist
    @PreUpdate
    public void updateLocation() {
        if (latitude != null && longitude != null) {
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
            this.location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }
    }

}
