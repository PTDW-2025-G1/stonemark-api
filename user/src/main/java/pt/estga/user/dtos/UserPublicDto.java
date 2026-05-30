package pt.estga.user.dtos;

import lombok.Builder;
import pt.estga.sharedweb.annotations.Filterable;

@Builder
public record UserPublicDto(
        Long id,
        @Filterable String username,
        @Filterable String firstName,
        @Filterable String lastName,
        Long photoId
) { }
