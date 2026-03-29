package pt.estga.bookmark;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.shared.enums.TargetType;
import pt.estga.monument.MonumentMapper;
import pt.estga.monument.MonumentRepository;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkQueryService bookmarkQueryService;
    private final MonumentRepository monumentRepository;
    private final MarkRepository markRepository;
    private final MonumentMapper monumentMapper;
    private final MarkMapper markMapper;
    private final BookmarkMapper mapper;
    private final UserRepository userRepository;

    @Transactional
    public BookmarkDto createBookmark(Long userId, TargetType type, String targetId) {

        if (bookmarkQueryService.isBookmarked(userId, type, targetId)) {
            throw new IllegalStateException("Bookmarked already exists");
        }

        Object content = switch (type) {
            case MONUMENT -> {
                Long parsedId = Long.parseLong(targetId);
                yield monumentRepository.findById(parsedId)
                        .map(monumentMapper::toResponseDto)
                        .orElseThrow(() -> new IllegalArgumentException("Monument not found"));
            }

            case MARK -> {
                Long parsedId = Long.parseLong(targetId);
                yield markRepository.findById(parsedId)
                        .map(markMapper::toDto)
                        .orElseThrow(() -> new IllegalArgumentException("Mark not found"));
            }

            default -> null;
        };

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .targetType(type)
                .targetId(targetId)
                .build();

        bookmarkRepository.save(bookmark);

        BookmarkDto dto = mapper.toDto(bookmark);
        return new BookmarkDto(dto.id(), dto.type(), dto.targetId(), content);
    }

    @Transactional
    public void deleteBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found"));
        bookmarkRepository.delete(bookmark);
    }
}
