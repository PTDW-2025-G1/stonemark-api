package pt.estga.mark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.Optional;

@Repository
public interface MarkOccurrenceRepository extends BaseRepository<MarkOccurrence, Long> {

    Optional<MarkOccurrence> findByMarkIdAndMonumentIdIsNull(Long markId);

}
