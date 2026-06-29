package pt.estga.user.dtos;

public record UserFilter(
        String username,
        String email,
        Boolean enabled
) {}
