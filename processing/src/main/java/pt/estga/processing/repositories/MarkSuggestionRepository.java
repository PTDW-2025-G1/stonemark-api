package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.processing.entities.MarkSuggestion;

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
}
