package pt.estga.user.dtos;

import lombok.Builder;
import pt.estga.sharedweb.annotations.Filterable;
import pt.estga.shared.enums.UserRole;

@Builder
public record UserPublicDto(
        Long id,
        @Filterable String username,
        @Filterable String firstName,
        @Filterable String lastName,
        @Filterable UserRole role,
        Long photoId
) { }
