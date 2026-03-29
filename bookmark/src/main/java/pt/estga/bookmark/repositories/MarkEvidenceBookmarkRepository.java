package pt.estga.bookmark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.bookmark.entities.MarkEvidenceBookmark;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarkEvidenceBookmarkRepository extends BaseRepository<MarkEvidenceBookmark, UUID> {

    List<MarkEvidenceBookmark> findAllByUserId(Long userId);

    Optional<MarkEvidenceBookmark> findByIdAndUserId(UUID id, Long userId);
}
