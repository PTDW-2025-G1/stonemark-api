package pt.estga.processing.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.processing.entities.SpatialCluster;
import pt.estga.processing.repositories.projections.SpatialClusterWithOccurrenceCount;
import pt.estga.shared.repositories.BaseRepository;

import java.util.Optional;

@Repository
public interface SpatialClusterRepository extends BaseRepository<SpatialCluster, Long> {

    @Query(value = """
        SELECT * FROM spatial_cluster sc
        WHERE sc.cluster_status = 'ACTIVE'
          AND ST_DWithin(
              sc.centroid,
              ST_SetSRID(ST_MakePoint(:lon, :lat), 4326),
              :radiusMeters
          )
        ORDER BY ST_Distance(sc.centroid, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
        LIMIT 1
    """, nativeQuery = true)
    Optional<SpatialCluster> findActiveWithinDistance(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters);

    @Query(value = """
        SELECT sc.id AS id, sc.label AS label,
               ST_Y(sc.centroid) AS centroidLatitude,
               ST_X(sc.centroid) AS centroidLongitude,
               sc.created_at AS createdAt,
               COALESCE(occ_cnt.cnt, 0) AS occurrenceCount
        FROM spatial_cluster sc
        LEFT JOIN LATERAL (
            SELECT COUNT(DISTINCT me.occurrence_id) AS cnt
            FROM mark_evidence_processing mep
            JOIN mark_evidence_submission mes ON mes.id = mep.submission_id
            JOIN mark_evidence me ON me.file_id = mes.original_media_file_id
            WHERE mep.spatial_cluster_id = sc.id
        ) occ_cnt ON true
        WHERE sc.cluster_status = 'ACTIVE'
        ORDER BY sc.created_at DESC
    """, countQuery = """
        SELECT COUNT(*) FROM spatial_cluster sc
        WHERE sc.cluster_status = 'ACTIVE'
    """, nativeQuery = true)
    Page<SpatialClusterWithOccurrenceCount> findActiveWithOccurrenceCount(Pageable pageable);
}
