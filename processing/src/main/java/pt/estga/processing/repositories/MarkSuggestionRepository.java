package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.estga.processing.entities.MarkSuggestion;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarkSuggestionRepository extends JpaRepository<MarkSuggestion, UUID>, JpaSpecificationExecutor<MarkSuggestion> {

	void deleteByProcessingId(UUID processingId);

	List<MarkSuggestion> findByProcessingId(UUID processingId);

	boolean existsByProcessingIdAndMarkId(UUID processingId, Long markId);
}
