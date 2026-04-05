package pt.estga.mark.repositories;

import pt.estga.mark.entities.MarkEvidence;
import pt.estga.shared.repositories.BaseRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarkEvidenceRepository extends BaseRepository<MarkEvidence, UUID> {

	/**
	 * The query expects the
	 * vector literal as a string (e.g. '[0.1,0.2,0.3]') and casts it to vector.
	 */
	@Query(value = "select (count(*) > 0) from mark_evidence me " +
			"where (:occurrenceId is null or me.occurrence_id = :occurrenceId) " +
			"and (me.embedding <-> CAST(:vector AS vector)) <= :maxDistance", nativeQuery = true)
	boolean existsByEmbeddingSimilar(@Param("vector") String vector,
									 @Param("occurrenceId") Long occurrenceId,
									 @Param("maxDistance") double maxDistance);

}
