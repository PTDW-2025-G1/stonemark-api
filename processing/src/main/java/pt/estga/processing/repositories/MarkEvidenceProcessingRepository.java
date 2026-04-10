package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pt.estga.processing.entities.MarkEvidenceProcessing;

import java.util.UUID;
import java.util.Optional;
import java.util.List;
import pt.estga.processing.enums.ProcessingStatus;

public interface MarkEvidenceProcessingRepository extends JpaRepository<MarkEvidenceProcessing, UUID> {

	boolean existsBySubmissionId(Long submissionId);

    Optional<MarkEvidenceProcessing> findBySubmissionId(Long submissionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM MarkEvidenceProcessing p WHERE p.submission.id = :submissionId")
	Optional<MarkEvidenceProcessing> findBySubmissionIdForUpdate(@Param("submissionId") Long submissionId);

	List<MarkEvidenceProcessing> findByStatusIn(List<ProcessingStatus> statuses);
}
