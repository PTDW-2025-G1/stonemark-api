package pt.estga.bookmark.dto;

import pt.estga.bookmark.enums.BookmarkTargetType;

/**
 * Request payload for creating a bookmark.
 */
public record BookmarkCreateRequest(
        BookmarkTargetType targetType,
        String targetId
) {
}
