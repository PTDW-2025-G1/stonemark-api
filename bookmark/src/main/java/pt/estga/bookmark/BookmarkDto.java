package pt.estga.bookmark;

import pt.estga.shared.enums.TargetType;

public record BookmarkDto(
        Long id,
        TargetType type,
        String targetId,
        Object content
) {}

