package pt.estga.mark.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.MarkOccurrence;

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
}
