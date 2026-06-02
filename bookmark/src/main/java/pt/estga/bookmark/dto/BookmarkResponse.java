package pt.estga.bookmark.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import pt.estga.bookmark.enums.BookmarkTargetType;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response representation for a bookmark with resolved target content.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookmarkResponse(
        @Schema(description = "Unique bookmark identifier.")
        UUID id,

        @Schema(description = "Type of the bookmarked resource.")
        BookmarkTargetType type,

        @Schema(description = "ID of the bookmarked resource.")
        String targetId,

        @Schema(description = "Timestamp when the bookmark was created.")
        Instant createdAt,

        @Schema(description = "Resolved target resource content. Type varies based on 'type' field.")
        BookmarkContent content
) {
}
