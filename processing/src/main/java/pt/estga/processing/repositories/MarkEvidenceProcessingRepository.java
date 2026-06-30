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
import pt.estga.processing.repositories.projections.RetryableProjection;

public interface MarkEvidenceProcessingRepository extends JpaRepository<MarkEvidenceProcessing, UUID> {

	boolean existsBySubmissionId(Long submissionId);

    Optional<MarkEvidenceProcessing> findBySubmissionId(Long submissionId);

	@Query("SELECT p.id AS id, p.status AS status FROM MarkEvidenceProcessing p WHERE p.submissionId = :submissionId")
	Optional<ProcessingOverviewProjection> findOverviewBySubmissionId(@Param("submissionId") Long submissionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM MarkEvidenceProcessing p WHERE p.submissionId = :submissionId")
	Optional<MarkEvidenceProcessing> findBySubmissionIdForUpdate(@Param("submissionId") Long submissionId);

	@Modifying
	@Query("UPDATE MarkEvidenceProcessing p SET p.status = :status WHERE p.submissionId = :submissionId")
	int updateStatusBySubmissionId(@Param("submissionId") Long submissionId, @Param("status") ProcessingStatus status);

    @Query("SELECT p.id AS id, p.submissionId AS submissionId, p.retryCount AS retryCount, p.lastRetryAt AS lastRetryAt FROM MarkEvidenceProcessing p WHERE p.status IN :statuses AND p.permanent = false AND p.retryCount < p.maxRetries")
    List<RetryableProjection> findRetryableProjectionsByStatusIn(@Param("statuses") List<ProcessingStatus> statuses);

    @Modifying
    @Query("UPDATE MarkEvidenceProcessing p SET p.status = :targetStatus, p.updatedAt = :now WHERE p.status = :sourceStatus AND p.updatedAt IS NOT NULL AND p.updatedAt < :cutoff")
    int resetStuckProcessing(@Param("sourceStatus") ProcessingStatus sourceStatus, @Param("targetStatus") ProcessingStatus targetStatus, @Param("cutoff") Instant cutoff, @Param("now") Instant now);

    @Query(value = "SELECT embedding::text FROM mark_evidence_processing WHERE submission_id = :submissionId AND embedding IS NOT NULL", nativeQuery = true)
    Optional<String> findEmbeddingTextBySubmissionId(@Param("submissionId") Long submissionId);

    @Query(value = "SELECT s.id FROM mark_evidence_submission s WHERE s.status = 'RECEIVED' AND NOT EXISTS (SELECT 1 FROM mark_evidence_processing p WHERE p.submission_id = s.id)", nativeQuery = true)
    List<Long> findOrphanedSubmissionIds();
}
