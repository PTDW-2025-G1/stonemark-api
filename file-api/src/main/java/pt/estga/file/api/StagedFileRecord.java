package pt.estga.file.api;

import java.util.UUID;

public record StagedFileRecord(
        UUID id,
        String originalFilename,
        long size
) { }
