package pt.estga.mark.services.mark;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkRequestDto;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkCommandService {

    private final MarkRepository repository;
    private final MarkMapper mapper;

    public Mark create(Mark mark) {
        return repository.save(mark);
    }

    public Mark create(MarkRequestDto dto) {
        Mark entity = mapper.toEntity(dto);
        return repository.save(entity);
    }

    public Mark update(Mark mark) {
        if (mark.getId() == null) {
            throw new ResourceNotFoundException("Mark id must not be null for update");
        }

        Mark existing = repository.findById(mark.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mark with id " + mark.getId() + " not found"));

        mapper.update(mark, existing);

        return repository.save(existing);
    }

    public Mark update(Long id, MarkRequestDto dto) {
        Mark existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mark with id " + id + " not found"));

        mapper.updateEntityFromDto(dto, existing);

        // coverId handled by mapper via MediaFileMapper when needed; mapper will
        // ignore id field changes and only update non-null properties.

        return repository.save(existing);
    }

    public void deleteById(Long id) {
        Mark existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mark with id " + id + " not found"));

        repository.softDelete(existing);
    }
}
