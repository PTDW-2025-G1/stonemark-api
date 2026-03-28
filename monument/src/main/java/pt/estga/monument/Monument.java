package pt.estga.monument;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
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

}
