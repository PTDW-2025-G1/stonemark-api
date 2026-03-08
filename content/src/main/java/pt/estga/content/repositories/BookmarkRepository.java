package pt.estga.content.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.content.entities.Bookmark;
import pt.estga.shared.enums.TargetType;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByUserId(Long userId);

    Optional<Bookmark> findByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);
}
