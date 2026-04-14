package pt.estga.mark.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.projections.EvidenceEmbeddingProjection;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarkEvidenceRepository extends BaseRepository<MarkEvidence, UUID> {

	@Query(value = """
	SELECT
		me.id,
		me.occurrence_id,
		(1.0 - (me.embedding <#> CAST(:vector AS vector))) AS similarity
	FROM mark_evidence me
 	WHERE (me.embedding <#> CAST(:vector AS vector)) <= :maxDistance
 	ORDER BY similarity DESC, me.id ASC, me.occurrence_id ASC
	LIMIT :k
	""", nativeQuery = true)
	List<MarkEvidenceDistanceProjection> findTopKSimilarEvidence(
			@Param("vector") String vector,
			@Param("k") int k,
			@Param("maxDistance") double maxDistance
	);

	@Query(value = """
	SELECT
		me.id,
		me.occurrence_id,
		(1.0 - (me.embedding <#> CAST(:vector AS vector))) AS similarity,
		(me.embedding <#> CAST(:vector AS vector)) AS distance
	FROM mark_evidence me
	ORDER BY distance ASC, me.id ASC, me.occurrence_id ASC
	LIMIT :k
	""", nativeQuery = true)
    List<MarkEvidenceDistanceProjection> findTopKSimilarEvidence(@Param("vector") String vector, @Param("k") int k);

	@Query("""
	SELECT e.id as id, m as mark FROM MarkEvidence e
	JOIN e.occurrence o
	JOIN o.mark m
	WHERE e.id IN :ids
	""")
	List<EvidenceMarkProjection> findMarksByEvidenceIds(@Param("ids") List<UUID> ids);

	Page<MarkEvidence> findAllByEmbeddingIsNotNull(Pageable pageable);

	// Projection-based batch fetch to return embeddings without hydrating full entities.
	List<EvidenceEmbeddingProjection> findAllByIdIn(List<UUID> ids);

	Optional<MarkEvidence> findByFileId(UUID fileId);
}
