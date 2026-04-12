package pt.estga.monument;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import pt.estga.monument.converters.MonumentValidationStateConverter;
import pt.estga.shared.entities.BaseEntity;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.monument.enums.MonumentValidationState;

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

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision division;

    @Builder.Default
    @Convert(converter = MonumentValidationStateConverter.class)
    @Column(nullable = false)
    private MonumentValidationState validationState = MonumentValidationState.VERIFIED;

}
