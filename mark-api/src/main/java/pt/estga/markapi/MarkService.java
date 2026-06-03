package pt.estga.markapi;

import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;

import java.util.Optional;

public interface MarkService {

    Optional<MarkDto> findMarkById(Long id);

    Optional<MarkOccurrenceDto> findOccurrenceById(Long id);
}
