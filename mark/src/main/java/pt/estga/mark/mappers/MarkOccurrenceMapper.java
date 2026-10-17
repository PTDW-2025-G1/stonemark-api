package pt.estga.mark.mappers;

import org.locationtech.jts.geom.Point;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.entities.MarkOccurrence;

public class MarkOccurrenceMapper {

    private MarkOccurrenceMapper() {}

    public static MarkOccurrenceDto toDto(MarkOccurrence entity) {
        if (entity == null) return null;
        Point location = entity.getLocation();
        return new MarkOccurrenceDto(
                entity.getId(),
                entity.getMark() != null ? entity.getMark().getId() : null,
                entity.getMonumentId(),
                location != null ? location.getY() : null,
                location != null ? location.getX() : null,
                entity.getPeriod(),
                entity.getStatus(),
                entity.getPublishedAt(),
                entity.getMark() != null ? MarkMapper.toDto(entity.getMark()) : null
        );
    }
}
