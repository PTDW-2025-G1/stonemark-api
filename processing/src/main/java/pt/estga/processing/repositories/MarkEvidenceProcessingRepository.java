package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pt.estga.processing.entities.MarkEvidenceProcessing;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.projections.ProcessingOverviewProjection;

public interface MarkEvidenceProcessingRepository extends JpaRepository<MarkEvidenceProcessing, UUID> {

	boolean existsBySubmissionId(Long submissionId);

	List<MarkEvidenceProcessing> findBySubmissionIdIn(List<Long> submissionIds);

	List<MarkEvidenceProcessing> findByReviewGroupId(Long reviewGroupId);

	long countByReviewGroupIdAndStatus(Long reviewGroupId, ProcessingStatus status);

    Optional<MarkEvidenceProcessing> findBySubmissionId(Long submissionId);

	@Query("SELECT p.id AS id, p.status AS status FROM MarkEvidenceProcessing p WHERE p.submissionId = :submissionId")
	Optional<ProcessingOverviewProjection> findOverviewBySubmissionId(@Param("submissionId") Long submissionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM MarkEvidenceProcessing p WHERE p.submissionId = :submissionId")
	Optional<MarkEvidenceProcessing> findBySubmissionIdForUpdate(@Param("submissionId") Long submissionId);

	@Modifying
	@Query("UPDATE MarkEvidenceProcessing p SET p.status = :status WHERE p.submissionId = :submissionId")
	int updateStatusBySubmissionId(@Param("submissionId") Long submissionId, @Param("status") ProcessingStatus status);

	List<MarkEvidenceProcessing> findByStatusIn(List<ProcessingStatus> statuses);

	@Query("SELECT p FROM MarkEvidenceProcessing p WHERE p.status IN :statuses AND p.permanent = false AND p.retryCount < p.maxRetries")
	List<MarkEvidenceProcessing> findRetryableByStatusIn(@Param("statuses") List<ProcessingStatus> statuses);

    long countByStatusAndPermanentFalse(ProcessingStatus status);

    @Modifying
    @Query("UPDATE MarkEvidenceProcessing p SET p.status = :targetStatus, p.updatedAt = :now WHERE p.status = :sourceStatus AND p.updatedAt IS NOT NULL AND p.updatedAt < :cutoff")
    int resetStuckProcessing(@Param("sourceStatus") ProcessingStatus sourceStatus, @Param("targetStatus") ProcessingStatus targetStatus, @Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
