package pt.estga.mark.dtos;

import java.util.UUID;

/**
 * Data transfer object for MarkEvidence entity.
 */
public record MarkEvidenceDto(
        UUID id,
        UUID fileId,
        Long occurrenceId,
        Long markId,
        float[] embedding
) {
}
