package pt.estga.mark.mappers;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.shared.enums.EntityStatus;

public class MarkMapper {

    private MarkMapper() {}

    public static MarkDto toDto(Mark mark) {
        if (mark == null) return null;
        MarkEvidence golden = mark.getGoldenExample();
        return new MarkDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                golden != null ? golden.getEmbedding() : null,
                golden != null ? golden.getFileId() : null,
                mark.getStatus() == EntityStatus.ACTIVE
        );
    }
}
