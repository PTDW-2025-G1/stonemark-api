package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkEvidenceRequestDto(
        UUID fileId,
        UUID occurrenceId,
        float[] embedding
) { }
