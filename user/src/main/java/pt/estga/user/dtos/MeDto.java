package pt.estga.user.dtos;

import pt.estga.shared.enums.UserRole;

public record MeDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean passwordSet,
        boolean enabled,
        boolean accountLocked
) {}
