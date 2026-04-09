package pt.estga.mark.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.shared.repositories.BaseRepository;

@Repository
public interface MarkOccurrenceRepository extends BaseRepository<MarkOccurrence, Long> {

    @Override
    @EntityGraph(attributePaths = {"monument", "mark", "author"})
    Page<MarkOccurrence> findAll(Pageable pageable);

    @Query("""
    SELECT o FROM MarkOccurrence o
    JOIN FETCH o.mark m
    WHERE o.id IN :ids
    """)
    List<MarkOccurrence> findAllWithMarkByIdIn(@Param("ids") List<Long> ids);
}
