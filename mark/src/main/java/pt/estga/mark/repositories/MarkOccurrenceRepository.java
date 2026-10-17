package pt.estga.mark.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarkOccurrenceRepository extends BaseRepository<MarkOccurrence, Long> {

    Optional<MarkOccurrence> findByMarkIdAndMonumentId(Long markId, Long monumentId);

    List<MarkOccurrence> findByMarkId(Long markId);

    List<MarkOccurrence> findByMarkIdAndStatus(Long markId, PublicationStatus status);

    @EntityGraph(attributePaths = "mark")
    List<MarkOccurrence> findByIdInAndStatusAndDeletedAtIsNull(Collection<Long> ids, PublicationStatus status);
}
