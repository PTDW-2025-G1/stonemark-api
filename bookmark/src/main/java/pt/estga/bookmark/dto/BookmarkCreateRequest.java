package pt.estga.bookmark.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import pt.estga.bookmark.enums.BookmarkTargetType;

@Schema(description = "Request payload for creating a bookmark.")
public record BookmarkCreateRequest(
        @Schema(description = "Type of the target resource.", example = "MARK")
        BookmarkTargetType targetType,

        @Schema(description = "ID of the target resource.", example = "123")
        UUID targetId
) {
}
