package pt.estga.territory.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.commoninfra.repositories.BaseRepository;
import pt.estga.territory.entities.AdministrativeDivision;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministrativeDivisionRepository extends BaseRepository<AdministrativeDivision, Long>, JpaSpecificationExecutor<AdministrativeDivision> {
    List<AdministrativeDivision> findByParentId(Long parentId);

    List<AdministrativeDivision> findAllByParentIsNull();

    @Query(value = "SELECT p.* FROM administrative_division p " +
            "JOIN administrative_division c ON ST_Contains(p.geometry, c.geometry) " +
            "WHERE c.id = :childId AND p.id != :childId " +
            "ORDER BY ST_Area(p.geometry) ASC LIMIT 1", nativeQuery = true)
    Optional<AdministrativeDivision> findParentByGeometry(@Param("childId") Long childId);

    /**
     * Returns all divisions whose geometry contains the given point, ordered by area ascending.
     * For finding the lowest-level division, use {@link #findLowestContainingDivision} instead.
     */
    @Query(value = "SELECT * FROM administrative_division d " +
            "WHERE ST_Contains(d.geometry, ST_SetSRID(ST_Point(:longitude, :latitude), 4326)) " +
            "ORDER BY ST_Area(d.geometry) ASC", nativeQuery = true)
    List<AdministrativeDivision> findByCoordinates(@Param("latitude") double latitude, @Param("longitude") double longitude);

    /**
     * Returns the lowest-level administrative division containing the point.
     * Uses the explicit hierarchy: finds the division that contains the point
     * AND has no child that also contains the point. Area is not considered.
     */
    @Query(value = "SELECT d.* FROM administrative_division d " +
            "WHERE ST_Contains(d.geometry, ST_SetSRID(ST_Point(:longitude, :latitude), 4326)) " +
            "AND NOT EXISTS (SELECT 1 FROM administrative_division child " +
            "WHERE child.parent_id = d.id " +
            "AND ST_Contains(child.geometry, ST_SetSRID(ST_Point(:longitude, :latitude), 4326)))",
            nativeQuery = true)
    Optional<AdministrativeDivision> findLowestContainingDivision(@Param("latitude") double latitude, @Param("longitude") double longitude);
}
