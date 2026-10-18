package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.bookmark.dto.BookmarkCreateRequest;
import pt.estga.bookmark.dto.BookmarkResponse;
import pt.estga.bookmark.entities.Bookmark;
import pt.estga.bookmark.enums.BookmarkTargetType;
import pt.estga.bookmark.repositories.BookmarkRepository;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.shared.web.exceptions.DuplicateResourceException;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MarkQueryService markQueryService;

    public Page<BookmarkResponse> listByUser(UUID userId, Pageable pageable) {
        Page<Bookmark> page = bookmarkRepository.findAllByUserId(userId, pageable);
        Map<UUID, MarkDto> marks = resolveMarks(page.getContent());
        Map<UUID, MarkOccurrenceDto> occurrences = resolveOccurrences(page.getContent());
        return page.map(bookmark -> toResponse(bookmark, marks, occurrences));
    }

    @Transactional
    public BookmarkResponse create(UUID userId, BookmarkCreateRequest request) {
        BookmarkTargetType type = request.targetType();
        UUID targetId = request.targetId();
        if (type == null || targetId == null) {
            throw new IllegalArgumentException("Bookmark target type and id are required");
        }
        requireAvailable(type, targetId);
        if (bookmarkRepository.existsByUserIdAndTargetTypeAndTargetId(userId, type, targetId)) {
            throw new DuplicateResourceException("Bookmark already exists");
        }

        Bookmark bookmark = Bookmark.builder()
                .userId(userId)
                .targetType(type)
                .targetId(targetId)
                .build();
        try {
            Bookmark saved = bookmarkRepository.save(bookmark);
            return toResponse(saved, resolveMarks(List.of(saved)), resolveOccurrences(List.of(saved)));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateResourceException("Bookmark already exists");
        }
    }

    @Transactional
    public void delete(UUID userId, UUID bookmarkId) {
        bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .ifPresent(bookmarkRepository::delete);
    }

    private void requireAvailable(BookmarkTargetType type, UUID targetId) {
        switch (type) {
            case MARK -> {
                if (markQueryService.findPublishedMarksByIds(List.of(targetId)).isEmpty()) {
                    throw new ResourceNotFoundException("Mark " + targetId + " not found");
                }
            }
            case MARK_OCCURRENCE -> {
                if (markQueryService.findPublishedOccurrencesByIds(List.of(targetId)).isEmpty()) {
                    throw new ResourceNotFoundException("Mark occurrence " + targetId + " not found");
                }
            }
        }
    }

    /**
     * Resolves the targets of a page of bookmarks in one query per target type.
     * Targets that are missing or no longer published are absent from the maps.
     */
    private Map<UUID, MarkDto> resolveMarks(List<Bookmark> bookmarks) {
        List<UUID> ids = targetIds(bookmarks, BookmarkTargetType.MARK);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return markQueryService.findPublishedMarksByIds(ids).stream()
                .collect(Collectors.toMap(MarkDto::id, Function.identity()));
    }

    private Map<UUID, MarkOccurrenceDto> resolveOccurrences(List<Bookmark> bookmarks) {
        List<UUID> ids = targetIds(bookmarks, BookmarkTargetType.MARK_OCCURRENCE);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return markQueryService.findPublishedOccurrencesByIds(ids).stream()
                .collect(Collectors.toMap(MarkOccurrenceDto::id, Function.identity()));
    }

    private static List<UUID> targetIds(List<Bookmark> bookmarks, BookmarkTargetType type) {
        return bookmarks.stream()
                .filter(bookmark -> bookmark.getTargetType() == type)
                .map(Bookmark::getTargetId)
                .distinct()
                .toList();
    }

    private static BookmarkResponse toResponse(Bookmark bookmark, Map<UUID, MarkDto> marks,
                                               Map<UUID, MarkOccurrenceDto> occurrences) {
        MarkDto mark = bookmark.getTargetType() == BookmarkTargetType.MARK
                ? marks.get(bookmark.getTargetId())
                : null;
        MarkOccurrenceDto occurrence = bookmark.getTargetType() == BookmarkTargetType.MARK_OCCURRENCE
                ? occurrences.get(bookmark.getTargetId())
                : null;
        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getTargetType(),
                bookmark.getTargetId(),
                bookmark.getCreatedAt(),
                mark,
                occurrence);
    }
}
