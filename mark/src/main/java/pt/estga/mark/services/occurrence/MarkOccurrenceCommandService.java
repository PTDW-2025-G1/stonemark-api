package pt.estga.mark.services.occurrence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkOccurrenceRequestDto;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class MarkOccurrenceCommandService {

    private final MarkOccurrenceRepository repository;
    private final MarkOccurrenceMapper mapper;

    @Transactional
    public MarkOccurrence create(MarkOccurrence occurrence) {
        return repository.save(occurrence);
    }

    public MarkOccurrence create(MarkOccurrenceRequestDto dto) {
        MarkOccurrence entity = mapper.toEntity(dto);
        return repository.save(entity);
    }

    @Transactional
    public MarkOccurrence update(MarkOccurrence occurrence) {
        if (occurrence.getId() == null) {
            throw new ResourceNotFoundException("Occurrence id must not be null for update");
        }

        MarkOccurrence existing = repository.findById(occurrence.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkOccurrence with id " + occurrence.getId() + " not found"));

        mapper.updateEntityFromDto(mapper.toDto(occurrence), existing);

        return repository.save(existing);
    }

    public MarkOccurrence update(Long id, MarkOccurrenceRequestDto dto) {
        MarkOccurrence existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkOccurrence with id " + id + " not found"));

        mapper.updateFromRequest(dto, existing);

        return repository.save(existing);
    }

    public void deleteById(Long id) {
        MarkOccurrence existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkOccurrence with id " + id + " not found"));

        repository.softDelete(existing);
    }
}
