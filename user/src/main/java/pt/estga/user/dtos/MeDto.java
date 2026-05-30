package pt.estga.user.dtos;

public record MeDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean passwordSet,
        boolean enabled,
        boolean accountLocked
) {}
