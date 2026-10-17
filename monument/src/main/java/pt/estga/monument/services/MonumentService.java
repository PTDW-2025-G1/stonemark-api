package pt.estga.monument.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.monument.dtos.MonumentDto;
import pt.estga.monument.dtos.MonumentFilter;
import pt.estga.monument.dtos.MonumentListDto;
import pt.estga.monument.dtos.MonumentRequestDto;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.mappers.MonumentMapper;
import pt.estga.monument.repositories.MonumentRepository;
import pt.estga.shared.jpa.jpa.SpecBuilder;
import pt.estga.shared.web.exceptions.ResourceNotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonumentService {

    private final MonumentRepository repository;
    private final MonumentMapper mapper;

    public Page<MonumentListDto> search(MonumentFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<Monument>()
                .like("name", filter.name());
        return repository.findAll(sb.build(), pageable).map(mapper::toListDto);
    }

    public Optional<MonumentDto> findById(Long id) {
        return repository.findById(id).map(mapper::toResponseDto);
    }

    public Page<MonumentListDto> findByPolygon(String geoJson, Pageable pageable) {
        return repository.findByPolygon(geoJson, pageable).map(mapper::toListDto);
    }

    @Transactional
    public MonumentDto create(MonumentRequestDto dto) {
        return mapper.toResponseDto(repository.save(mapper.toEntity(dto)));
    }

    @Transactional
    public MonumentDto update(Long id, MonumentRequestDto dto) {
        Monument existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monument with id " + id + " not found"));
        mapper.updateEntityFromDto(dto, existing);
        return mapper.toResponseDto(repository.save(existing));
    }

    @Transactional
    public void deleteById(Long id) {
        Monument existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monument with id " + id + " not found"));

        repository.softDelete(existing);
    }
}
