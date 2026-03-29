package pt.estga.monument;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.shared.enums.EntityStatus;
import pt.estga.shared.repositories.BaseRepository;

import java.util.Optional;

@Repository
public interface MonumentRepository extends BaseRepository<Monument, Long>, JpaSpecificationExecutor<Monument> {

    @EntityGraph(attributePaths = {"division"})
    Page<Monument> findByDivisionIdAndStatus(Long divisionId, Pageable pageable, EntityStatus status);

    default Page<Monument> findByDivisionId(Long divisionId, Pageable pageable) {
        return findByDivisionIdAndStatus(divisionId, pageable, EntityStatus.ACTIVE);
    }

    Optional<Monument> findByExternalId(String externalId);

    @Query(value = "SELECT * FROM monument m WHERE ST_Within(m.location, ST_GeomFromGeoJSON(:geoJson))", nativeQuery = true)
    Page<Monument> findByPolygon(@Param("geoJson") String geoJson, Pageable pageable);
}
