package pt.estga.mark.repositories;

import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarkEvidenceRepository extends BaseRepository<MarkEvidence, UUID> {

	/**
	 * Nearest non-deleted evidence to a query embedding, closest first, restricted to
	 * candidates no further than the given cosine distance.
	 *
	 * <p>Spring Data vector search: Hibernate translates the derived query to pgvector's
	 * cosine operator, so there is no hand-written SQL or vector literal. The returned
	 * score is the raw cosine distance (lower is better); callers convert it to a
	 * similarity.
	 */
	SearchResults<MarkEvidence> findByEmbeddingNearAndOccurrenceIsNotNullAndDeletedAtIsNull(
			Vector embedding,
			Score distance,
			Limit limit);

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
