package pt.estga.bookmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pt.estga.bookmark.enums.BookmarkTargetType;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;

import java.time.Instant;
import java.util.UUID;

/**
 * A bookmark with its resolved target. The field matching the bookmark's
 * {@code type} is populated only while the target is available (published); a null
 * target means it is no longer available.
 */
@Schema(description = "A bookmark with its resolved target content.")
public record BookmarkResponse(
        @Schema(description = "Unique bookmark identifier.")
        UUID id,

        @Schema(description = "Type of the bookmarked resource.")
        BookmarkTargetType type,

        @Schema(description = "ID of the bookmarked resource.")
        Long targetId,

        @Schema(description = "Timestamp when the bookmark was created.")
        Instant createdAt,

        @Schema(description = "The bookmarked mark, or null when it is no longer available.")
        MarkDto mark,

        @Schema(description = "The bookmarked occurrence, or null when it is no longer available.")
        MarkOccurrenceDto occurrence
) {
}
