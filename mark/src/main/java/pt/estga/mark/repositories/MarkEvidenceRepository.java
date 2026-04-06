package pt.estga.mark.repositories;

import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
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

	/**
	 * Finds top-k most similar Marks by comparing the provided vector to the
	 * mark.canonical_embedding column using the pgvector "<->" distance operator.
	 * The method expects the vector as a Postgres vector literal string (for
	 * example: '[0.1,0.2,0.3]') which will be cast to the vector type in SQL.
	 */
	@Query(value = "SELECT m.* FROM mark m " +
			"ORDER BY (m.canonical_embedding <-> CAST(:vector AS vector)) ASC " +
			"LIMIT :k", nativeQuery = true)
	List<Mark> findTopKSimilar(@Param("vector") String vector, @Param("k") int k);

}
