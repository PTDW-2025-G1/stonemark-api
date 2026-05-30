package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkEvidenceDistanceDto(
        UUID id,
        Long occurrenceId,
        Double similarity
) {
}
