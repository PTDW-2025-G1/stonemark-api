package pt.estga.processing.entities;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import pt.estga.processing.enums.SpatialClusterStatus;
import pt.estga.shared.entities.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SpatialCluster extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point centroid;

    private Double radiusMeters;

    private String label;

    @Enumerated(EnumType.STRING)
    private SpatialClusterStatus clusterStatus;

    @Builder.Default
    @OneToMany(mappedBy = "spatialCluster")
    private List<MarkEvidenceProcessing> members = new ArrayList<>();
}
