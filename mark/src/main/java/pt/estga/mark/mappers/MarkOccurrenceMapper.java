package pt.estga.mark.mappers;

import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.commoncore.enums.EntityStatus;

public class MarkOccurrenceMapper {

    private MarkOccurrenceMapper() {}

    public static MarkOccurrenceDto toDto(MarkOccurrence entity) {
        if (entity == null) return null;
        return new MarkOccurrenceDto(
                entity.getId(),
                entity.getMark() != null ? entity.getMark().getId() : null,
                entity.getMonument() != null ? entity.getMonument().getId() : null,
                entity.getMark() != null ? MarkMapper.toDto(entity.getMark()) : null,
                entity.getCreatedAt(),
                entity.getStatus() == EntityStatus.ACTIVE
        );
    }
}
