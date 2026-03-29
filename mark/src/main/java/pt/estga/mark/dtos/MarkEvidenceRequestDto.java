package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkEvidenceRequestDto(
        UUID fileId,
        Long occurrenceId,
        float[] embedding
) { }
