package pt.estga.fileapi;

import java.util.UUID;

public record StagedFileRecord(
        UUID id,
        String originalFilename,
        long size
) { }