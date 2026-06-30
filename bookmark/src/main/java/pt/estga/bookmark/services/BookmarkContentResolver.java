package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.bookmark.dto.BookmarkContent;
import pt.estga.bookmark.dto.MarkBookmarkContent;
import pt.estga.bookmark.dto.MarkEvidenceBookmarkContent;
import pt.estga.bookmark.dto.MarkOccurrenceBookmarkContent;
import pt.estga.bookmark.entities.Bookmark;
import pt.estga.bookmark.enums.BookmarkTargetType;
import pt.estga.mark.api.MarkQueryService;
import pt.estga.mark.dtos.MarkEvidenceDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookmarkContentResolver {

    private final MarkQueryService markQueryService;

    public Map<UUID, BookmarkContent> resolve(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BookmarkContent> result = new HashMap<>();
        Map<BookmarkTargetType, List<Bookmark>> grouped = bookmarks.stream()
                .collect(Collectors.groupingBy(Bookmark::getTargetType));
        grouped.forEach((type, group) -> {
            switch (type) {
                case MARK -> resolveMarks(group, result);
                case MARK_OCCURRENCE -> resolveOccurrences(group, result);
                case MARK_EVIDENCE -> resolveEvidences(group, result);
            }
        });
        return result;
    }

    private void resolveMarks(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        for (Bookmark b : bookmarks) {
            markQueryService.findMarkById(Long.parseLong(b.getTargetId()))
                    .ifPresent(dto -> result.put(b.getId(), new MarkBookmarkContent(dto)));
        }
    }

    private void resolveOccurrences(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        for (Bookmark b : bookmarks) {
            markQueryService.findOccurrenceById(Long.parseLong(b.getTargetId()))
                    .ifPresent(dto -> result.put(b.getId(), new MarkOccurrenceBookmarkContent(dto)));
        }
    }

    private void resolveEvidences(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        List<UUID> ids = bookmarks.stream()
                .map(b -> UUID.fromString(b.getTargetId()))
                .toList();
        Map<UUID, MarkEvidenceDto> evidenceMap = markQueryService.findEvidenceByIds(ids).stream()
                .collect(Collectors.toMap(MarkEvidenceDto::id, Function.identity()));
        for (Bookmark b : bookmarks) {
            MarkEvidenceDto dto = evidenceMap.get(UUID.fromString(b.getTargetId()));
            if (dto != null) {
                result.put(b.getId(), new MarkEvidenceBookmarkContent(dto));
            }
        }
    }
}
