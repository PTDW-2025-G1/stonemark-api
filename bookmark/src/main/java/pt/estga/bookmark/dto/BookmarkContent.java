package pt.estga.bookmark.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MonumentBookmarkContent.class, name = "MONUMENT"),
        @JsonSubTypes.Type(value = MarkBookmarkContent.class, name = "MARK"),
        @JsonSubTypes.Type(value = MarkOccurrenceBookmarkContent.class, name = "MARK_OCCURRENCE"),
        @JsonSubTypes.Type(value = MarkEvidenceBookmarkContent.class, name = "MARK_EVIDENCE")
})
public sealed interface BookmarkContent
        permits MonumentBookmarkContent, MarkBookmarkContent, MarkOccurrenceBookmarkContent, MarkEvidenceBookmarkContent {
}
