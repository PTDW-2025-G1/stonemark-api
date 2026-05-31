package pt.estga.mark.mappers;

import org.springframework.stereotype.Component;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.entities.MarkEvidence;

@Component
public class MarkEvidenceMapper {

    public MarkEvidenceDto toDto(MarkEvidence evidence) {
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
