package pt.estga.mark.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.dtos.MarkOccurrenceUpdateDto;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.shared.core.utils.GeometryUtils;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkOccurrenceService {

    private final MarkOccurrenceRepository repository;

    public Page<MarkOccurrenceDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(MarkOccurrenceMapper::toDto);
    }

    public MarkOccurrenceDto findById(UUID id) {
        return repository.findById(id)
                .map(MarkOccurrenceMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark occurrence with id " + id + " not found"));
    }

    @Transactional
    public MarkOccurrenceDto update(UUID id, MarkOccurrenceUpdateDto dto) {
        MarkOccurrence occurrence = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark occurrence with id " + id + " not found"));
        if (dto.period() != null) {
            occurrence.setPeriod(dto.period());
        }
        if (dto.monumentId() != null) {
            occurrence.setMonumentId(dto.monumentId());
        }
        if (dto.latitude() != null || dto.longitude() != null) {
            occurrence.setLocation(GeometryUtils.createPoint(dto.latitude(), dto.longitude()));
        }
        if (dto.status() != null) {
            switch (dto.status()) {
                case PUBLISHED -> occurrence.publish();
                case ARCHIVED -> occurrence.archive();
                case DRAFT -> occurrence.revertToDraft();
            }
        }
        return MarkOccurrenceMapper.toDto(repository.save(occurrence));
    }

    @Transactional
    public void deleteById(UUID id) {
        MarkOccurrence occurrence = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark occurrence with id " + id + " not found"));
        repository.softDelete(occurrence);
    }
}
