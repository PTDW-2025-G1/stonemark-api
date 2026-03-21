package pt.estga.content.repositories;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.content.entities.Monument;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonumentRepository extends JpaRepository<Monument, Long> {

    @EntityGraph(attributePaths = {"district", "parish", "municipality"})
    Optional<Monument> findById(Long id);

    Optional<Monument> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {"district", "parish", "municipality"})
    @Query("SELECT m FROM Monument m WHERE m.active = :active")
    Page<Monument> findAllWithDivisions(Pageable pageable, @Param("active") boolean active);

    Page<Monument> findByNameContainingIgnoreCaseAndActive(String name, Pageable pageable, boolean active);

    Page<Monument> findByActive(Pageable pageable, boolean active);

    @Query(value = "SELECT * FROM monument m WHERE ST_Within(m.location, ST_GeomFromGeoJSON(:geoJson)) AND m.active = :active", nativeQuery = true)
    Page<Monument> findByPolygon(@Param("geoJson") String geoJson, Pageable pageable, @Param("active") boolean active);

    @Query("SELECT m FROM Monument m WHERE within(m.location, :geometry) = true AND m.active = :active")
    Page<Monument> findByGeometry(@Param("geometry") Geometry geometry, Pageable pageable, @Param("active") boolean active);

    @Query(value = "SELECT * FROM monument m WHERE ST_DWithin(m.location, ST_SetSRID(ST_Point(:longitude, :latitude), 4326), :range) AND m.active = :active", nativeQuery = true)
    List<Monument> findByCoordinatesInRange(@Param("latitude") double latitude, @Param("longitude") double longitude, @Param("range") double range, @Param("active") boolean active);

    @Query("SELECT m FROM MarkOccurrence mo JOIN mo.monument m WHERE m.active = :active GROUP BY m ORDER BY COUNT(m) DESC")
    List<Monument> findPopular(Pageable pageable, @Param("active") boolean active);
}
