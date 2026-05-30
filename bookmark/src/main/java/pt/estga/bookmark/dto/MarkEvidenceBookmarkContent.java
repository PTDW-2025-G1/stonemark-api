package pt.estga.bookmark.dto;

import pt.estga.mark.dtos.MarkEvidenceDto;

public record MarkEvidenceBookmarkContent(MarkEvidenceDto evidence) implements BookmarkContent {
}
