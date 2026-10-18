package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkEvidenceDistanceDto(
        UUID id,
        UUID occurrenceId,
        Double similarity
) {
}
