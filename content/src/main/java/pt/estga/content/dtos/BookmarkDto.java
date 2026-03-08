package pt.estga.content.dtos;

import pt.estga.shared.enums.TargetType;

public record BookmarkDto(
        Long id,
        TargetType type,
        Long targetId,
        Object content
) {}

