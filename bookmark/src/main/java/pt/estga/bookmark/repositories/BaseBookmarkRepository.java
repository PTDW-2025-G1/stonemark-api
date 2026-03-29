package pt.estga.bookmark.repositories;

import pt.estga.bookmark.entities.BaseBookmark;
import pt.estga.shared.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseBookmarkRepository extends BaseRepository<BaseBookmark, UUID> {

    List<BaseBookmark> findAllByCreatedById(Long userId);

    Optional<BaseBookmark> findByIdAndCreatedById(UUID id, Long userId);
}
