package pt.estga.monument.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentMapper;
import pt.estga.monument.MonumentRepository;
import pt.estga.monument.dots.MonumentRequestDto;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class MonumentCommandService {

    private final MonumentRepository repository;
    private final MonumentMapper mapper;

    @Transactional
    public Monument create(Monument monument) {
        return repository.save(monument);
    }

    public Monument create(MonumentRequestDto dto) {
        Monument entity = mapper.toEntity(dto);
        return repository.save(entity);
    }

    @Transactional
    public Monument update(Monument monument) {
        if (monument.getId() == null) {
            throw new ResourceNotFoundException("Monument id must not be null for update");
        }

        Monument existing = repository.findById(monument.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Monument with id " + monument.getId() + " not found"));

        mapper.update(monument, existing);

        return repository.save(existing);
    }

    public Monument update(Long id, MonumentRequestDto dto) {
        Monument existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Monument with id " + id + " not found"));

        mapper.updateEntityFromDto(dto, existing);

        return repository.save(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        Monument existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Monument with id " + id + " not found"));

        repository.softDelete(existing);
    }
}
