package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkUpdateDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkService {

    private final MarkRepository repository;

    public MarkDto findById(Long id) {
        return repository.findById(id)
                .map(MarkMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
    }

    @Transactional
    public MarkDto update(Long id, MarkUpdateDto dto) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        mark.setTitle(dto.title());
        mark.setDescription(dto.description());
        mark.setCoverMediaId(dto.coverMediaId());
        if (dto.status() != null) {
            switch (dto.status()) {
                case PUBLISHED -> mark.publish();
                case ARCHIVED -> mark.archive();
                case DRAFT -> mark.revertToDraft();
            }
        }
        return MarkMapper.toDto(repository.save(mark));
    }

    @Transactional
    public void deleteById(Long id) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        repository.softDelete(mark);
    }
}
