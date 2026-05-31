package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.processing.entities.ReviewGroup;
import pt.estga.processing.enums.ReviewGroupStatus;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewGroupRepository extends BaseRepository<ReviewGroup, Long> {

    List<ReviewGroup> findByGroupStatus(ReviewGroupStatus status);

    @Query(value = """
        SELECT rg.* FROM review_group rg
        WHERE rg.group_status = 'OPEN'
          AND rg.centroid IS NOT NULL
          AND ST_DWithin(
              rg.centroid,
              ST_SetSRID(ST_MakePoint(:lon, :lat), 4326),
              :radiusMeters
          )
        ORDER BY rg.member_count DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<ReviewGroup> findOpenGroupNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );

    Optional<ReviewGroup> findByIdAndGroupStatus(Long id, ReviewGroupStatus status);
}
