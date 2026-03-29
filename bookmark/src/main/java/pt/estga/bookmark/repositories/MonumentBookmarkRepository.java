package pt.estga.bookmark.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.bookmark.entities.MonumentBookmark;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonumentBookmarkRepository extends BaseRepository<MonumentBookmark, UUID> {

    List<MonumentBookmark> findAllByUserId(Long userId);

    Optional<MonumentBookmark> findByIdAndUserId(UUID id, Long userId);
}
