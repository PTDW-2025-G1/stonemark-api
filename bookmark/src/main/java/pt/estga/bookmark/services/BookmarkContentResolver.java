package pt.estga.bookmark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.bookmark.dto.*;
import pt.estga.bookmark.entities.Bookmark;
import pt.estga.bookmark.enums.BookmarkTargetType;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookmarkContentResolver {

    private final MarkRepository markRepository;
    private final MarkOccurrenceRepository occurrenceRepository;
    private final MarkEvidenceRepository evidenceRepository;

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
            markRepository.findById(Long.parseLong(b.getTargetId()))
                    .map(MarkMapper::toDto)
                    .ifPresent(dto -> result.put(b.getId(), new MarkBookmarkContent(dto)));
        }
    }

    private void resolveOccurrences(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        for (Bookmark b : bookmarks) {
            occurrenceRepository.findById(Long.parseLong(b.getTargetId()))
                    .map(MarkOccurrenceMapper::toDto)
                    .ifPresent(dto -> result.put(b.getId(), new MarkOccurrenceBookmarkContent(dto)));
        }
    }

    private void resolveEvidences(List<Bookmark> bookmarks, Map<UUID, BookmarkContent> result) {
        List<UUID> ids = bookmarks.stream()
                .map(b -> UUID.fromString(b.getTargetId()))
                .toList();
        Map<UUID, MarkEvidenceDto> evidenceMap = evidenceRepository.findAllById(ids).stream()
                .map(MarkEvidenceMapper::toDto)
                .collect(Collectors.toMap(MarkEvidenceDto::id, Function.identity()));
        for (Bookmark b : bookmarks) {
            MarkEvidenceDto dto = evidenceMap.get(UUID.fromString(b.getTargetId()));
            if (dto != null) {
                result.put(b.getId(), new MarkEvidenceBookmarkContent(dto));
            }
        }
    }
}
