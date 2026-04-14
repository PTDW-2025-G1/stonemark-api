package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.repositories.projections.ProcessingModerationProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarkSuggestionRepository extends JpaRepository<MarkSuggestion, UUID> {

	void deleteByProcessingId(UUID processingId);

	List<MarkSuggestion> findByProcessingId(UUID processingId);

	Optional<MarkSuggestion> findByProcessingIdAndMarkId(UUID processingId, Long markId);

	boolean existsByProcessingIdAndMarkId(UUID processingId, Long markId);

    long countByProcessingId(UUID processingId);

	@Query("SELECT MAX(s.confidence) FROM MarkSuggestion s WHERE s.processing.id = :processingId")
	Double findMaxConfidenceByProcessingId(@Param("processingId") UUID processingId);

	@Query("SELECT s.processing.id AS processingId, s.processing.submission.id AS submissionId, s.processing.status AS status, MAX(s.confidence) AS maxConfidence " +
			"FROM MarkSuggestion s " +
			"GROUP BY s.processing.id, s.processing.submission.id, s.processing.status " +
			"HAVING MAX(s.confidence) BETWEEN :min AND :max")
	java.util.List<ProcessingModerationProjection> findProcessingByMaxConfidenceBetween(@Param("min") double min, @Param("max") double max);
}
