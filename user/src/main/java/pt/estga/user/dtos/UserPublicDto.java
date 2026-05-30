package pt.estga.user.dtos;

import lombok.Builder;

@Builder
public record UserPublicDto(
        Long id,
        String username,
        String firstName,
        String lastName,
        Long photoId
) { }
