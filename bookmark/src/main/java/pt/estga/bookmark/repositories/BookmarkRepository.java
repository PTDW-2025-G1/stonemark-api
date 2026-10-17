package pt.estga.bookmark.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.estga.bookmark.entities.Bookmark;
import pt.estga.bookmark.enums.BookmarkTargetType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    Page<Bookmark> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndTargetTypeAndTargetId(UUID userId, BookmarkTargetType targetType, UUID targetId);

    Optional<Bookmark> findByIdAndUserId(UUID id, UUID userId);
}
