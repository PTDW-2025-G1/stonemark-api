package pt.estga.processing.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.shared.repositories.BaseRepository;

import java.util.Optional;

public interface DraftMarkEvidenceRepository extends BaseRepository<DraftMarkEvidence, Long> {

	DraftMarkEvidence findBySubmissionId(Long submissionId);

	/**
	 * Find by id and acquire a pessimistic write lock at the database level.
	 * This approach relies on Spring Data JPA @Lock rather than manually using an EntityManager.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DraftMarkEvidence d where d.id = :id")
	Optional<DraftMarkEvidence> findByIdForUpdate(@Param("id") Long id);
}
