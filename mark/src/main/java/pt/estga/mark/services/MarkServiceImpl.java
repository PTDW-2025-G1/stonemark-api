package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkUpdateDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.markapi.MarkService;
import pt.estga.shared.enums.EntityStatus;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkServiceImpl implements MarkService {

    private final MarkRepository repository;

    @Override
    public Page<MarkDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    @Override
    public MarkDto findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
    }

    @Override
    @Transactional
    public MarkDto update(Long id, MarkUpdateDto dto) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        mark.setTitle(dto.title());
        mark.setDescription(dto.description());
        if (dto.active() != null) {
            if (dto.active()) {
                mark.activate();
            } else {
                mark.deactivate();
            }
        }
        return toDto(repository.save(mark));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Mark mark = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mark with id " + id + " not found"));
        repository.softDelete(mark);
    }

    private MarkDto toDto(Mark mark) {
        return new MarkDto(
                mark.getId(),
                mark.getTitle(),
                mark.getDescription(),
                null,
                null,
                mark.getStatus() == EntityStatus.ACTIVE
        );
    }
}
