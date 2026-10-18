package pt.estga.monument.repositories;

import java.util.UUID;

import pt.estga.monument.entities.Monument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.shared.jpa.repositories.BaseRepository;

@Repository
public interface MonumentRepository extends BaseRepository<Monument, UUID>, JpaSpecificationExecutor<Monument> {

    @Query(value = "SELECT * FROM monument m WHERE ST_Within(m.location, ST_GeomFromGeoJSON(:geoJson))", nativeQuery = true)
    Page<Monument> findByPolygon(@Param("geoJson") String geoJson, Pageable pageable);
}
