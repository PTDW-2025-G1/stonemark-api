package pt.estga.mark.api;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarkQueryService {

    Optional<MarkDto> findMarkById(Long id);

    Optional<MarkOccurrenceDto> findOccurrenceById(Long id);

    List<MarkEvidenceDto> findEvidenceByIds(List<UUID> ids);
}
