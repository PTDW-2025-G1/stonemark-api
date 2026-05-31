package pt.estga.bookmark.dto;

import pt.estga.monument.dtos.MonumentDto;

public record MonumentBookmarkContent(MonumentDto monument) implements BookmarkContent {
}
