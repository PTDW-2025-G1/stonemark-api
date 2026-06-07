package pt.estga.mark.mappers;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.sharedcore.enums.EntityStatus;

public class MarkMapper {

    private MarkMapper() {}

    public static MarkDto toDto(Mark mark) {
        if (mark == null) return null;
        MarkEvidence exemplar = mark.getExemplar();
        return new MarkDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                exemplar != null ? exemplar.getEmbedding() : null,
                exemplar != null ? exemplar.getFileId() : null,
                mark.getStatus() == EntityStatus.ACTIVE
        );
    }
}
