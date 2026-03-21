package pt.estga.content.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.content.entities.Mark;
import pt.estga.content.entities.MarkOccurrence;
import pt.estga.content.entities.Monument;
import pt.estga.content.repositories.projections.MarkSimilarityProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkOccurrenceRepository extends JpaRepository<MarkOccurrence, Long> {

    @EntityGraph(attributePaths = {"monument", "mark", "author"})
    Page<MarkOccurrence> findByActiveIsTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"monument", "mark", "author"})
    Page<MarkOccurrence> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"monument", "mark"})
    Page<MarkOccurrence> findByMarkIdAndActiveIsTrue(Long markId, Pageable pageable);

    @EntityGraph(attributePaths = {"monument.district", "monument.parish", "monument.municipality"})
    @Query("SELECT mo FROM MarkOccurrence mo WHERE mo.mark.id = :markId AND mo.active = true")
    List<MarkOccurrence> findAllByMarkIdForMap(@Param("markId") Long markId);

    @EntityGraph(attributePaths = {"monument", "mark"})
    Page<MarkOccurrence> findByMonumentIdAndActiveIsTrue(Long monumentId, Pageable pageable);

    @EntityGraph(attributePaths = {"monument", "mark"})
    @Query("SELECT mo FROM MarkOccurrence mo WHERE mo.active = true ORDER BY mo.createdAt DESC")
    List<MarkOccurrence> findLatest(Pageable pageable);

    @Query("SELECT DISTINCT mo.mark FROM MarkOccurrence mo WHERE mo.monument.id = :monumentId AND mo.active = true")
    List<Mark> findDistinctMarksByMonumentId(@Param("monumentId") Long monumentId);

    @Query("SELECT DISTINCT mo.monument FROM MarkOccurrence mo WHERE mo.mark.id = :markId AND mo.active = true")
    List<Monument> findDistinctMonumentsByMarkId(@Param("markId") Long markId);

    @EntityGraph(attributePaths = {"monument", "mark"})
    Page<MarkOccurrence> findByMarkIdAndMonumentIdAndActiveIsTrue(Long markId, Long monumentId, Pageable pageable);

    @Query("SELECT COUNT(mo) FROM MarkOccurrence mo WHERE mo.monument.id = :monumentId AND mo.active = true")
    long countByMonumentId(Long monumentId);

    @Query("SELECT COUNT(mo) FROM MarkOccurrence mo WHERE mo.mark.id = :markId AND mo.active = true")
    long countByMarkId(Long markId);

    @Query("SELECT COUNT(DISTINCT mo.monument.id) FROM MarkOccurrence mo WHERE mo.mark.id = :markId AND mo.active = true")
    long countDistinctMonumentIdByMarkId(@Param("markId") Long markId);

    @EntityGraph(attributePaths = {"monument.district", "monument.parish", "monument.municipality", "mark", "author"})
    Optional<MarkOccurrence> findById(Long id);

    @Query(value = "SELECT id, 1 - (embedding <=> CAST(:vector AS vector)) as similarity " +
            "FROM mark_occurrence " +
            "WHERE embedding IS NOT NULL AND active = true " +
            "ORDER BY similarity DESC " +
            "LIMIT 5", nativeQuery = true)
    List<MarkSimilarityProjection> findSimilarOccurrences(@Param("vector") float[] vector);
}
