package pt.estga.bookmark.dto;

import pt.estga.monument.dots.MonumentDto;

public record MonumentBookmarkContent(MonumentDto monument) implements BookmarkContent {
}
