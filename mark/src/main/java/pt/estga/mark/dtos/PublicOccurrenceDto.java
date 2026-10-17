package pt.estga.mark.dtos;

import java.util.List;
import java.util.UUID;

/**
 * A published finding of a mark. Coordinates are optional: an occurrence may be
 * catalogued without a precise location (an "unlocated" entry).
 */
public record PublicOccurrenceDto(
        Long id,
        Long monumentId,
        Double latitude,
        Double longitude,
        String period,
        List<UUID> mediaIds
) { }
