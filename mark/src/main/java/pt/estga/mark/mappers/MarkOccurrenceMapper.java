package pt.estga.mark.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.shared.enums.EntityStatus;

@Component
@RequiredArgsConstructor
public class MarkOccurrenceMapper {

    private final MarkMapper markMapper;

    public MarkOccurrenceDto toDto(MarkOccurrence entity) {
        if (entity == null) return null;
        return new MarkOccurrenceDto(
                entity.getId(),
                entity.getMark() != null ? entity.getMark().getId() : null,
                entity.getMonument() != null ? entity.getMonument().getId() : null,
                entity.getMark() != null ? markMapper.toDto(entity.getMark()) : null,
                null,
                entity.getStatus() == EntityStatus.ACTIVE
        );
    }
}
