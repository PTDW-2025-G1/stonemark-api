package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.bookmark.dto.BookmarkContent;
import pt.estga.bookmark.dto.BookmarkCreateRequest;
import pt.estga.bookmark.dto.BookmarkResponse;
import pt.estga.bookmark.entities.Bookmark;
import pt.estga.bookmark.repositories.BookmarkRepository;
import pt.estga.commonweb.exceptions.DuplicateResourceException;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;
import pt.estga.user.api.UserQueryService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkContentResolver contentResolver;
    private final UserQueryService userQueryService;

    public Page<BookmarkResponse> listByUser(Long userId, Pageable pageable) {
        Page<Bookmark> page = bookmarkRepository.findAllByCreatedById(userId, pageable);
        Map<UUID, BookmarkContent> contentMap = contentResolver.resolve(page.getContent());
        return page.map(b -> toResponse(b, contentMap.get(b.getId())));
    }

    @Transactional
    public BookmarkResponse create(Long userId, BookmarkCreateRequest request) {
        if (bookmarkRepository.existsByCreatedByIdAndTargetTypeAndTargetId(
                userId, request.targetType(), request.targetId())) {
            throw new DuplicateResourceException("Bookmark already exists");
        }
        if (!userQueryService.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        Bookmark bookmark = Bookmark.builder()
                .createdById(userId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .build();

        Bookmark saved = bookmarkRepository.save(bookmark);
        BookmarkContent content = resolveSingle(saved);
        return toResponse(saved, content);
    }

    @Transactional
    public void delete(Long userId, UUID bookmarkId) {
        bookmarkRepository.findByIdAndCreatedById(bookmarkId, userId)
                .ifPresent(bookmarkRepository::delete);
    }

    private BookmarkContent resolveSingle(Bookmark bookmark) {
        Map<UUID, BookmarkContent> map = contentResolver.resolve(List.of(bookmark));
        return map.get(bookmark.getId());
    }

    private static BookmarkResponse toResponse(Bookmark b, BookmarkContent content) {
        return new BookmarkResponse(b.getId(), b.getTargetType(), b.getTargetId(), b.getCreatedAt(), content);
    }
}
