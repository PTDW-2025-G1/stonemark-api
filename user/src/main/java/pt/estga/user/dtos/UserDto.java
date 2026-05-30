package pt.estga.user.dtos;

import lombok.*;
import pt.estga.file.dtos.MediaFileDto; // file-api module

import java.time.Instant;
import java.util.Set;

@Builder
public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String username,
        String email,
        MediaFileDto photo,
        Set<String> roles,
        Instant createdAt
) { }
