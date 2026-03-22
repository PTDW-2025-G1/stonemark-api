package pt.estga.mark.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.projections.MarkSimilarityProjection;

import java.util.List;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {

    @Query(value = "SELECT id, 1 - (embedding <=> CAST(:vector AS vector)) as similarity " +
            "FROM mark " +
            "WHERE embedding IS NOT NULL AND active = true " +
            "ORDER BY similarity DESC " +
            "LIMIT 5", nativeQuery = true)
    List<MarkSimilarityProjection> findSimilarMarks(@Param("vector") float[] vector);
}
