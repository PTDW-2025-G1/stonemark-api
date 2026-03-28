package pt.estga.territory.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.shared.repositories.BaseRepository;
import pt.estga.territory.entities.AdministrativeDivision;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministrativeDivisionRepository extends BaseRepository<AdministrativeDivision, Long>, JpaSpecificationExecutor<AdministrativeDivision> {
    List<AdministrativeDivision> findByOsmAdminLevel(int osmAdminLevel);

    List<AdministrativeDivision> findByParentId(Long parentId);

    List<AdministrativeDivision> findAllByParentIsNull();

    @Query(value = "SELECT p.* FROM administrative_division p " +
            "JOIN administrative_division c ON ST_Intersects(p.geometry, c.geometry) " +
            "WHERE c.id = :childId AND p.osm_admin_level = :parentLevel " +
            "ORDER BY ST_Area(ST_Intersection(p.geometry, c.geometry)) DESC LIMIT 1", nativeQuery = true)
    Optional<AdministrativeDivision> findParentByGeometry(@Param("childId") Long childId, @Param("parentLevel") int parentLevel);

    @Query(value = "SELECT * FROM administrative_division d " +
            "WHERE ST_Contains(d.geometry, ST_SetSRID(ST_Point(:longitude, :latitude), 4326)) " +
            "ORDER BY d.osm_admin_level ASC", nativeQuery = true)
    List<AdministrativeDivision> findByCoordinates(@Param("latitude") double latitude, @Param("longitude") double longitude);

    @Query("SELECT d FROM AdministrativeDivision d WHERE d.osmAdminLevel = :adminLevel AND d.monumentsCount > 0")
    List<AdministrativeDivision> findWithMonuments(@Param("adminLevel") int adminLevel);
}
