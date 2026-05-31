package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import pt.estga.processing.enums.ReviewGroupStatus;
import pt.estga.shared.entities.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "review_group")
public class ReviewGroup extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private ReviewGroupStatus groupStatus;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point centroid;

    private Double radiusMeters;

    @Column(columnDefinition = "vector(384)")
    private float[] meanEmbedding;

    @Column(nullable = false)
    private Integer memberCount;

    private Integer decision;

    private Instant reviewedAt;

    private Long reviewedById;

    private String comment;

    @Builder.Default
    @OneToMany(mappedBy = "reviewGroup")
    private List<MarkEvidenceProcessing> members = new ArrayList<>();
}
