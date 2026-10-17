package pt.estga.user.dtos;

import pt.estga.file.dtos.MediaFileDto;
import pt.estga.user.enums.UserRole;

import java.time.Instant;

public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String username,
        String email,
        MediaFileDto photo,
        UserRole role,
        Instant createdAt
) { }
