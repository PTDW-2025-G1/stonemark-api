package pt.estga.mark.mappers;

import pt.estga.mark.dtos.PublicMarkDto;
import pt.estga.mark.dtos.PublicMarkSummaryDto;
import pt.estga.mark.dtos.PublicOccurrenceDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkOccurrence;

import java.util.List;
import java.util.UUID;

public class PublicMarkMapper {

    private PublicMarkMapper() {}

    public static PublicMarkSummaryDto toSummary(Mark mark) {
        if (mark == null) return null;
        return new PublicMarkSummaryDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                mark.getCoverMediaId()
        );
    }

    public static PublicMarkDto toDetail(Mark mark, List<PublicOccurrenceDto> occurrences) {
        if (mark == null) return null;
        return new PublicMarkDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                mark.getCoverMediaId(),
                occurrences
        );
    }

    public static PublicOccurrenceDto toOccurrence(MarkOccurrence occurrence, List<UUID> mediaIds) {
        if (occurrence == null) return null;
        var location = occurrence.getLocation();
        return new PublicOccurrenceDto(
                occurrence.getId(),
                occurrence.getMonumentId(),
                location != null ? location.getY() : null,
                location != null ? location.getX() : null,
                occurrence.getPeriod(),
                mediaIds
        );
    }
}
