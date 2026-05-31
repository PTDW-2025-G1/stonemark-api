package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.bookmark.dto.*;
import pt.estga.bookmark.entities.Bookmark;
import pt.estga.bookmark.enums.BookmarkTargetType;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.markapi.MarkService;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentMapper;
import pt.estga.monument.MonumentRepository;
import pt.estga.monument.dtos.MonumentDto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookmarkContentResolver {

    private final MonumentRepository monumentRepository;
    private final MonumentMapper monumentMapper;
    private final MarkService markService;

    public Map<UUID, BookmarkContent> resolve(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BookmarkContent> result = new HashMap<>();
        Map<BookmarkTargetType, List<Bookmark>> grouped = bookmarks.stream()
                .collect(Collectors.groupingBy(Bookmark::getTargetType));
        grouped.forEach((type, group) -> {
            switch (type) {
                case MONUMENT -> resolveMonuments(group, result);
                case MARK -> resolveMarks(group, result);
                case MARK_OCCURRENCE -> resolveOccurrences(group, result);
                case MARK_EVIDENCE -> resolveEvidences(group, result);
            }
        });
        return result;
    }

    private void resolveMonuments(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        List<Long> ids = bookmarks.stream()
                .map(b -> Long.parseLong(b.getTargetId()))
                .toList();
        Map<Long, MonumentDto> monumentMap = monumentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Monument::getId, monumentMapper::toResponseDto));
        for (Bookmark b : bookmarks) {
            MonumentDto dto = monumentMap.get(Long.parseLong(b.getTargetId()));
            if (dto != null) {
                result.put(b.getId(), new MonumentBookmarkContent(dto));
            }
        }
    }

    private void resolveMarks(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        for (Bookmark b : bookmarks) {
            markService.findMarkById(Long.parseLong(b.getTargetId()))
                    .ifPresent(dto -> result.put(b.getId(), new MarkBookmarkContent(dto)));
        }
    }

    private void resolveOccurrences(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        for (Bookmark b : bookmarks) {
            markService.findOccurrenceById(Long.parseLong(b.getTargetId()))
                    .ifPresent(dto -> result.put(b.getId(), new MarkOccurrenceBookmarkContent(dto)));
        }
    }

    private void resolveEvidences(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        List<UUID> ids = bookmarks.stream()
                .map(b -> UUID.fromString(b.getTargetId()))
                .toList();
        Map<UUID, MarkEvidenceDto> evidenceMap = markService.findEvidenceByIdIn(ids).stream()
                .collect(Collectors.toMap(MarkEvidenceDto::id, Function.identity()));
        for (Bookmark b : bookmarks) {
            MarkEvidenceDto dto = evidenceMap.get(UUID.fromString(b.getTargetId()));
            if (dto != null) {
                result.put(b.getId(), new MarkEvidenceBookmarkContent(dto));
            }
        }
    }
}
