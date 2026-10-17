package pt.estga.monument.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import pt.estga.shared.jpa.entities.SoftDeletableEntity;

/**
 * A place: a building, wall, church or other site where marks are found.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Monument extends SoftDeletableEntity {

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

    /** A monument is either usable in the catalogue or withdrawn; this is not a publish lifecycle. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
