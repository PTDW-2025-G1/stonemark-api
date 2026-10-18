package pt.estga.user.dtos;

import java.util.UUID;

import pt.estga.file.dtos.MediaFileDto;
import pt.estga.user.enums.UserRole;

import java.time.Instant;

public record UserDto(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        MediaFileDto photo,
        UserRole role,
        Instant createdAt
) { }
