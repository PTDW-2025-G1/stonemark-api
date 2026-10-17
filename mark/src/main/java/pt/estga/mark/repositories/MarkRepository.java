package pt.estga.mark.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.estga.mark.entities.Mark;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkRepository extends BaseRepository<Mark, Long>, JpaSpecificationExecutor<Mark> {

	List<Mark> findByStatus(PublicationStatus status);

	Optional<Mark> findByIdAndStatus(Long id, PublicationStatus status);
}
