package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.processing.entities.SpatialCluster;
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
}
