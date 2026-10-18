package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkEvidenceDto(
        UUID id,
        UUID fileId,
        UUID occurrenceId,
        UUID markId,
        UUID submissionId,
        float[] embedding
) {
}
