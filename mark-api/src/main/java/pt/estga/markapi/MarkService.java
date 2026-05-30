package pt.estga.markapi;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;

import java.util.Optional;
import java.util.UUID;

public interface MarkService {

    Optional<MarkDto> findMarkById(Long id);

    Optional<MarkOccurrenceDto> findOccurrenceById(Long id);

    Optional<MarkEvidenceDto> findEvidenceById(UUID id);
}
