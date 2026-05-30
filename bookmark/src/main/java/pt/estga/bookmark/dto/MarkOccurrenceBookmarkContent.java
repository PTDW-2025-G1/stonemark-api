package pt.estga.bookmark.dto;

import pt.estga.mark.dtos.MarkOccurrenceDto;

public record MarkOccurrenceBookmarkContent(MarkOccurrenceDto occurrence) implements BookmarkContent {
}
