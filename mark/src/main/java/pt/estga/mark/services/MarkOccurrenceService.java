package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkOccurrenceService {

    private final MarkOccurrenceRepository repository;

    public MarkOccurrenceDto findById(Long id) {
        return repository.findById(id)
                .map(MarkOccurrenceMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark occurrence with id " + id + " not found"));
    }

    @Transactional
    public void deleteById(Long id) {
        MarkOccurrence occurrence = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark occurrence with id " + id + " not found"));
        repository.softDelete(occurrence);
    }
}
