package pt.estga.mark.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarkOccurrenceRepository extends BaseRepository<MarkOccurrence, UUID> {

    Optional<MarkOccurrence> findByMarkIdAndMonumentId(UUID markId, UUID monumentId);

    List<MarkOccurrence> findByMarkId(UUID markId);

    List<MarkOccurrence> findByMarkIdAndStatus(UUID markId, PublicationStatus status);

    @EntityGraph(attributePaths = "mark")
    List<MarkOccurrence> findByIdInAndStatusAndDeletedAtIsNull(Collection<UUID> ids, PublicationStatus status);
}
