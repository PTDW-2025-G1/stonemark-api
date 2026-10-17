package pt.estga.review.enums;

public enum MarkCreationSuggestionStatus {
    /** Awaiting a moderator decision. */
    OPEN,
    /** A moderator created a mark from this cluster. */
    ACTIONED,
    /** A moderator dismissed this cluster; it will not be suggested again. */
    DISMISSED
}
