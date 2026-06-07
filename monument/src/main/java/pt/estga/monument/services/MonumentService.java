package pt.estga.monument.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.monument.dtos.MonumentFilter;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.commoninfra.jpa.SpecBuilder;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonumentService {

    private final MonumentRepository repository;

    public Optional<Monument> findById(Long id) {
        return repository.findById(id);
    }

    public Page<Monument> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Monument> search(MonumentFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<Monument>()
                .eq("division.id", filter.divisionId())
                .like("name", filter.name());
        return repository.findAll(sb.build(), pageable);
    }

    public Page<Monument> findByPolygon(String geoJson, Pageable pageable) {
        return repository.findByPolygon(geoJson, pageable);
    }

    public Page<Monument> findByDivisionId(Long id, Pageable pageable) {
        return repository.findByDivisionId(id, pageable);
    }

    @Transactional
    public Monument save(Monument monument) {
        return repository.save(monument);
    }

    @Transactional
    public void deleteById(Long id) {
        Monument existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monument with id " + id + " not found"));

        repository.softDelete(existing);
    }
}
