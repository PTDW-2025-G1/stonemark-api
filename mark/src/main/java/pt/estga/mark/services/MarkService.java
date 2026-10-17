package pt.estga.mark.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkUpdateDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.shared.core.enums.PublicationStatus;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkService {

    private final MarkRepository repository;
    private final MarkOccurrenceRepository occurrenceRepository;

    public Page<MarkDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(MarkMapper::toDto);
    }

    public MarkDto findById(UUID id) {
        return repository.findById(id)
                .map(MarkMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
    }

    /**
     * Applies an admin edit. Publishing a mark for the first time cascade-publishes
     * its existing occurrences; occurrences added afterwards keep their own lifecycle.
     */
    @Transactional
    public MarkDto update(UUID id, MarkUpdateDto dto) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        mark.setTitle(dto.title());
        mark.setDescription(dto.description());
        mark.setCoverMediaId(dto.coverMediaId());
        PublicationStatus target = dto.status();
        if (target != null) {
            boolean firstPublish = target == PublicationStatus.PUBLISHED
                    && mark.getStatus() != PublicationStatus.PUBLISHED;
            switch (target) {
                case PUBLISHED -> mark.publish();
                case ARCHIVED -> mark.archive();
                case DRAFT -> mark.revertToDraft();
            }
            if (firstPublish) {
                occurrenceRepository.findByMarkId(id).forEach(MarkOccurrence::publish);
            }
        }
        return MarkMapper.toDto(repository.save(mark));
    }

    @Transactional
    public void deleteById(UUID id) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        repository.softDelete(mark);
    }
}
