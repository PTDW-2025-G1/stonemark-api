package pt.estga.content.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.content.entities.MarkOccurrence;
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

    @EntityGraph(attributePaths = {"monument.district", "monument.parish", "monument.municipality", "mark", "author"})
    Optional<MarkOccurrence> findById(Long id);

    @Query(value = "SELECT id, 1 - (embedding <=> CAST(:vector AS vector)) as similarity " +
            "FROM mark_occurrence " +
            "WHERE embedding IS NOT NULL AND active = true " +
            "ORDER BY similarity DESC " +
            "LIMIT 5", nativeQuery = true)
    List<MarkSimilarityProjection> findSimilarOccurrences(@Param("vector") float[] vector);
}
