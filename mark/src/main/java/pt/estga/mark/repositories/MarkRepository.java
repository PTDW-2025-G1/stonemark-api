package pt.estga.mark.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.Mark;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarkRepository extends BaseRepository<Mark, UUID>, JpaSpecificationExecutor<Mark> {

	List<Mark> findByStatus(PublicationStatus status);

	Optional<Mark> findByIdAndStatus(UUID id, PublicationStatus status);

	List<Mark> findByIdInAndStatusAndDeletedAtIsNull(Collection<UUID> ids, PublicationStatus status);
}
