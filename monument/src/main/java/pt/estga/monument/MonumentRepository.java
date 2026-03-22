package pt.estga.monument;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonumentRepository extends JpaRepository<Monument, Long> {

    @EntityGraph(attributePaths = {"district", "parish", "municipality"})
    Optional<Monument> findById(Long id);

    @EntityGraph(attributePaths = {"district", "parish", "municipality"})
    Page<Monument> findByDistrictIdOrMunicipalityIdOrParishIdAndActive(Long districtId, Long municipalityId, Long parishId, Pageable pageable, boolean active);

    default Page<Monument> findByDivisionId(Long divisionId, Pageable pageable, boolean active) {
        return findByDistrictIdOrMunicipalityIdOrParishIdAndActive(divisionId, divisionId, divisionId, pageable, active);
    }

    Optional<Monument> findByExternalId(String externalId);

    Page<Monument> findByNameContainingIgnoreCaseAndActive(String name, Pageable pageable, boolean active);

    @Query(value = "SELECT * FROM monument m WHERE ST_Within(m.location, ST_GeomFromGeoJSON(:geoJson)) AND m.active = :active", nativeQuery = true)
    Page<Monument> findByPolygon(@Param("geoJson") String geoJson, Pageable pageable, @Param("active") boolean active);
}
