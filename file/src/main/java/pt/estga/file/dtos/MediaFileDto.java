package pt.estga.file.dtos;

import pt.estga.file.enums.MediaStatus;

import java.util.UUID;

public record MediaFileDto(
        UUID id,
        String filename,
        String originalFilename,
        Long size,
        MediaStatus status
) { }
