package pt.estga.user.dtos;

import lombok.*;
import pt.estga.file.dtos.MediaFileDto;
import pt.estga.shared.enums.UserRole;

import java.time.Instant;

@Builder
public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String username,
        String email,
        String phone,
        boolean emailVerified,
        boolean phoneVerified,
        MediaFileDto photo,
        UserRole role,
        Instant createdAt
) { }
