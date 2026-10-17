package pt.estga.mark.mappers;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.entities.Mark;

public class MarkMapper {

    private MarkMapper() {}

    public static MarkDto toDto(Mark mark) {
        if (mark == null) return null;
        return new MarkDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                mark.getCoverMediaId(),
                mark.getStatus()
        );
    }
}
