package pt.estga.content.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.dtos.BookmarkDto;
import pt.estga.content.entities.Bookmark;
import pt.estga.content.mappers.BookmarkMapper;
import pt.estga.content.mappers.MarkMapper;
import pt.estga.content.mappers.MonumentMapper;
import pt.estga.content.repositories.BookmarkRepository;
import pt.estga.content.repositories.MarkRepository;
import pt.estga.content.repositories.MonumentRepository;
import pt.estga.content.enums.TargetType;
import pt.estga.user.entities.User;
import pt.estga.user.repositories.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MonumentRepository monumentRepository;
    private final MarkRepository markRepository;
    private final MonumentMapper monumentMapper;
    private final MarkMapper markMapper;
    private final BookmarkMapper mapper;
    private final UserRepository userRepository;

    @Transactional
    public BookmarkDto createBookmark(Long userId, TargetType type, String targetId) {

        bookmarkRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Bookmark already exists");
                });

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

    public List<BookmarkDto> getUserBookmarks(Long userId) {
        return bookmarkRepository.findAllByUserId(userId)
                .stream()
                .map(b -> {
                    Object content = switch (b.getTargetType()) {
                        case MONUMENT -> {
                            Long parsedId = Long.parseLong(b.getTargetId());
                            yield monumentRepository.findById(parsedId)
                                    .map(monumentMapper::toResponseDto)
                                    .orElse(null);
                        }

                        case MARK -> {
                            Long parsedId = Long.parseLong(b.getTargetId());
                            yield markRepository.findById(parsedId)
                                    .map(markMapper::toDto)
                                    .orElse(null);
                        }

                        default -> null;
                    };
                    BookmarkDto dto = mapper.toDto(b);
                    return new BookmarkDto(dto.id(), dto.type(), dto.targetId(), content);
                })
                .toList();
    }

    public boolean isBookmarked(Long userId, TargetType type, String targetId) {
        return bookmarkRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, type, targetId)
                .isPresent();
    }

    @Transactional
    public void deleteBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found"));
        bookmarkRepository.delete(bookmark);
    }
}
