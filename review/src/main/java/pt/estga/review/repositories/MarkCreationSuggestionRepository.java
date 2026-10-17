package pt.estga.review.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.review.entities.MarkCreationSuggestion;
import pt.estga.review.enums.MarkCreationSuggestionStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarkCreationSuggestionRepository extends JpaRepository<MarkCreationSuggestion, UUID> {

    Page<MarkCreationSuggestion> findByStatus(MarkCreationSuggestionStatus status, Pageable pageable);

    List<MarkCreationSuggestion> findByStatus(MarkCreationSuggestionStatus status);

    void deleteByStatus(MarkCreationSuggestionStatus status);
}
