package pt.estga.bookmark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.bookmark.entities.MarkOccurrenceBookmark;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarkOccurrenceBookmarkRepository extends BaseRepository<MarkOccurrenceBookmark, UUID> {

    List<MarkOccurrenceBookmark> findAllByCreatedById(Long userId);

    Optional<MarkOccurrenceBookmark> findByIdAndCreatedById(UUID id, Long userId);

    boolean existsByCreatedByIdAndMarkOccurrenceId(Long userId, Long occurrenceId);
}
