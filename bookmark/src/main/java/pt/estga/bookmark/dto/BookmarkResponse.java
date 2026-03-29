package pt.estga.bookmark.dto;

import pt.estga.bookmark.enums.BookmarkTargetType;

import java.time.Instant;
import java.util.UUID;

/**
 * Response representation for a bookmark.
 */
public record BookmarkResponse(
        UUID id,
        BookmarkTargetType type,
        String targetId,
        Instant createdAt,
        Object content
) {
}
