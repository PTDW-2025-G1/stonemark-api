package pt.estga.bookmark;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.enums.TargetType;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.monument.MonumentMapper;
import pt.estga.monument.MonumentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkQueryService {

    private final BookmarkRepository bookmarkRepository;
    private final MonumentRepository monumentRepository;
    private final MarkRepository markRepository;
    private final MonumentMapper monumentMapper;
    private final MarkMapper markMapper;
    private final BookmarkMapper mapper;

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
}
