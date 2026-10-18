package pt.estga.mark.repositories;

import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarkEvidenceRepository extends BaseRepository<MarkEvidence, UUID> {

	// Cosine similarity via pgvector's cosine distance (<=>): similarity = 1 - distance.
	// <#> is the negative inner product, so 1 - (embedding <#> vector) would yield
	// 1 + cosine for normalized vectors (range [0,2]) and a meaningless threshold.
	@Query(value = """
	SELECT
		me.id,
		me.occurrence_id,
		(1.0 - (me.embedding <=> CAST(:vector AS vector))) AS similarity
	FROM mark_evidence me
 	WHERE me.embedding IS NOT NULL
 	  AND me.deleted_at IS NULL
 	  AND (me.embedding <=> CAST(:vector AS vector)) <= :maxDistance
 	ORDER BY similarity DESC, me.id ASC, me.occurrence_id ASC
	LIMIT :k
	""", nativeQuery = true)
	List<MarkEvidenceDistanceProjection> findTopKSimilarEvidence(
			@Param("vector") String vector,
			@Param("k") int k,
			@Param("maxDistance") double maxDistance
	);

	@Query("""
	SELECT e.id as id, m as mark FROM MarkEvidence e
	JOIN e.occurrence o
	JOIN o.mark m
	WHERE e.id IN :ids
	""")
	List<EvidenceMarkProjection> findMarksByEvidenceIds(@Param("ids") List<UUID> ids);

	Optional<MarkEvidence> findByFileId(UUID fileId);

	List<MarkEvidence> findByOccurrenceIdIn(Collection<UUID> occurrenceIds);
}
