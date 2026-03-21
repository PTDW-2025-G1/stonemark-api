package pt.estga.content.dtos;

import pt.estga.content.enums.TargetType;

public record BookmarkDto(
        Long id,
        TargetType type,
        String targetId,
        Object content
) {}

