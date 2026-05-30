package pt.estga.file.dtos;

import java.util.UUID;

public record MediaFileDto(
        UUID id,
        String filename,
        String originalFilename,
        Long size,
        String status
) { }
