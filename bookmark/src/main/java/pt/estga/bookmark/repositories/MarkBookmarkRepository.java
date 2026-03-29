package pt.estga.bookmark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.bookmark.entities.MarkBookmark;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarkBookmarkRepository extends BaseRepository<MarkBookmark, UUID> {

    List<MarkBookmark> findAllByCreatedById(Long userId);

    Optional<MarkBookmark> findByIdAndCreatedById(UUID id, Long userId);

    boolean existsByCreatedByIdAndMarkId(Long userId, Long markId);
}
