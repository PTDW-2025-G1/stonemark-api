package pt.estga.mark.repositories;

import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarkEvidenceRepository extends BaseRepository<MarkEvidence, UUID> {

	@Query(value = """
	SELECT
		me.id,
		me.occurrence_id,
		(me.embedding <#> CAST(:vector AS vector)) AS distance
	FROM mark_evidence me
	WHERE (:occurrenceId IS NULL OR me.occurrence_id = :occurrenceId)
	ORDER BY distance ASC
	LIMIT :k
	""", nativeQuery = true)
	List<MarkEvidenceDistanceProjection> findTopKSimilarEvidence(
			@Param("vector") String vector,
			@Param("occurrenceId") Long occurrenceId,
			@Param("k") int k
	);

	@Query("""
	SELECT e.id as id, m as mark FROM MarkEvidence e
	JOIN e.occurrence o
	JOIN o.mark m
	WHERE e.id IN :ids
	""")
	List<EvidenceMarkProjection> findMarksByEvidenceIds(@Param("ids") List<UUID> ids);
}
