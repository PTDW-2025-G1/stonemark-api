package pt.estga.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.shared.enums.TargetType;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByUserId(Long userId);

    List<Bookmark> findByUserIdAndTargetType(Long userId, TargetType targetType);

    Optional<Bookmark> findByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, String targetId);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);
}
