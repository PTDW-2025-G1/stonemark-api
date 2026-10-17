package pt.estga.mark.entities;

import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import pt.estga.shared.jpa.entities.PublishableEntity;

import java.util.List;

/**
 * A physical finding of a {@link Mark} at a place.
 *
 * <p>An occurrence may be tied to a monument, to its own coordinates, or to
 * neither (catalogued without a precise location). Both a mark and an occurrence
 * carry their own {@code PublicationStatus}.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkOccurrence extends PublishableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Mark mark;

    /** Optional monument where the mark was found. */
    @Column(name = "monument_id")
    private UUID monumentId;

    /**
     * Optional WGS84 point of the finding. A null location is valid and means the
     * occurrence is catalogued without coordinates.
     */
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    /** Heritage era or dating note for this occurrence. */
    private String period;

    @OneToMany(mappedBy = "occurrence", cascade = CascadeType.MERGE)
    private List<MarkEvidence> evidences;
}
