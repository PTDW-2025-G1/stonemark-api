package pt.estga.mark.mappers;

import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.entities.MarkEvidence;

public class MarkEvidenceMapper {

    private MarkEvidenceMapper() {}

    public static MarkEvidenceDto toDto(MarkEvidence evidence) {
        if (evidence == null) return null;
        return new MarkEvidenceDto(
                evidence.getId(),
                evidence.getFileId(),
                evidence.getOccurrence() != null ? evidence.getOccurrence().getId() : null,
                evidence.getOccurrence() != null && evidence.getOccurrence().getMark() != null
                        ? evidence.getOccurrence().getMark().getId() : null,
                evidence.getEmbedding()
        );
    }
}
