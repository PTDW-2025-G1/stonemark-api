package pt.estga.user.dtos;

import java.util.UUID;

import lombok.Builder;

@Builder
public record UserPublicDto(
        UUID id,
        String username,
        String firstName,
        String lastName,
        UUID photoId
) { }
