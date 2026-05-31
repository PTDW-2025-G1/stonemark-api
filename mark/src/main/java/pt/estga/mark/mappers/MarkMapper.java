package pt.estga.mark.mappers;

import org.springframework.stereotype.Component;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.entities.Mark;
import pt.estga.shared.enums.EntityStatus;

@Component
public class MarkMapper {

    public MarkDto toDto(Mark mark) {
        if (mark == null) return null;
        return new MarkDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                null,
                null,
                mark.getStatus() == EntityStatus.ACTIVE
        );
    }
}
