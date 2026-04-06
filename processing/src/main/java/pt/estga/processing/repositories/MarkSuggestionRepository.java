package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.processing.entities.MarkSuggestion;

import java.util.UUID;

public interface MarkSuggestionRepository extends JpaRepository<MarkSuggestion, UUID> {
}
