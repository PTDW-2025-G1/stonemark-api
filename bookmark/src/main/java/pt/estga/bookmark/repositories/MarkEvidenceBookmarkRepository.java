package pt.estga.bookmark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.bookmark.entities.MarkEvidenceBookmark;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarkEvidenceBookmarkRepository extends BaseRepository<MarkEvidenceBookmark, UUID> {

    List<MarkEvidenceBookmark> findAllByCreatedById(Long userId);

    Optional<MarkEvidenceBookmark> findByIdAndCreatedById(UUID id, Long userId);

    boolean existsByCreatedByIdAndMarkEvidenceId(Long userId, java.util.UUID evidenceId);
}
