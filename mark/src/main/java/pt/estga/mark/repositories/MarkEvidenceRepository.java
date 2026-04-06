package pt.estga.mark.repositories;

import pt.estga.mark.entities.MarkEvidence;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarkEvidenceRepository extends BaseRepository<MarkEvidence, UUID> {

	@Query(value = """
    SELECT me.* FROM mark_evidence me
    WHERE (:occurrenceId IS NULL OR me.occurrence_id = :occurrenceId)
    ORDER BY (me.embedding <-> CAST(:vector AS vector)) ASC
    LIMIT :k
	""", nativeQuery = true)
	List<MarkEvidence> findTopKSimilarEvidence(
			@Param("vector") String vector,
			@Param("occurrenceId") Long occurrenceId,
			@Param("k") int k
	);
}
