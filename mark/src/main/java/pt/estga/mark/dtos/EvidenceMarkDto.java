package pt.estga.mark.dtos;

import java.util.UUID;

public record EvidenceMarkDto(
        UUID evidenceId,
        UUID markId
) {
}
