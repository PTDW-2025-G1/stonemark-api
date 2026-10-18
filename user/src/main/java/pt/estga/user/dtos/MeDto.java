package pt.estga.user.dtos;

import java.util.UUID;

public record MeDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean passwordSet,
        boolean enabled,
        boolean accountLocked
) {}
