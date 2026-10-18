package pt.estga.mark.entities;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pt.estga.shared.jpa.entities.PublishableEntity;

import java.util.List;
import java.util.UUID;

/**
 * A catalogued type of mason's mark (for example a "cross" or "compass" pattern).
 *
 * <p>The mark is the abstract pattern; the physical findings of that pattern are
 * modelled by {@link MarkOccurrence}s.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Mark extends PublishableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private String title;

    private String description;

    /** Optional representative image, resolved through the file module. */
    @Column(name = "cover_media_id")
    private UUID coverMediaId;

    @OneToMany(mappedBy = "mark")
    private List<MarkOccurrence> occurrences;
}
