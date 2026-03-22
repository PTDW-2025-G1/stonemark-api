package pt.estga.bookmark;

import pt.estga.mark.enums.TargetType;

public record BookmarkDto(
        Long id,
        TargetType type,
        String targetId,
        Object content
) {}

