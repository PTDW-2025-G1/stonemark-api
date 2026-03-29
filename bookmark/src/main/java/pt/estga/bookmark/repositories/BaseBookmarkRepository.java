package pt.estga.bookmark.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.bookmark.entities.BaseBookmark;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseBookmarkRepository extends JpaRepository<BaseBookmark, UUID> {

    List<BaseBookmark> findAllByUserId(Long userId);

    Optional<BaseBookmark> findByIdAndUserId(UUID id, Long userId);
}
