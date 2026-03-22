package pt.estga.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.content.enums.TargetType;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByUserId(Long userId);

    Optional<Bookmark> findByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, String targetId);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);
}
