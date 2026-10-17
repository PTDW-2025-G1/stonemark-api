package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkEvidenceDto(
        UUID id,
        UUID fileId,
        Long occurrenceId,
        Long markId,
        float[] embedding
) {
}
