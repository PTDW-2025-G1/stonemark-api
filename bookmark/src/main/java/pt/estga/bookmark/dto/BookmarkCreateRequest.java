package pt.estga.bookmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pt.estga.bookmark.enums.BookmarkTargetType;

@Schema(description = "Request payload for creating a bookmark.")
public record BookmarkCreateRequest(
        @Schema(description = "Type of the target resource.", example = "MONUMENT")
        BookmarkTargetType targetType,

        @Schema(description = "ID of the target resource (numeric for MONUMENT/MARK/MARK_OCCURRENCE, UUID for MARK_EVIDENCE).",
                example = "123")
        String targetId
) {
}
