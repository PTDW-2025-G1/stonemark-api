package pt.estga.monument.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.monument.dtos.MonumentFilter;
import pt.estga.monument.entities.Monument;
import pt.estga.monument.repositories.MonumentRepository;
import pt.estga.commoninfra.jpa.SpecBuilder;
import pt.estga.commonweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonumentService {

    private final MonumentRepository repository;

    public Page<Monument> search(MonumentFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<Monument>()
                .eq("divisionCode", filter.divisionCode())
                .like("name", filter.name());
        return repository.findAll(sb.build(), pageable);
    }

    @Transactional
    public void deleteById(Long id) {
        Monument existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monument with id " + id + " not found"));

        repository.softDelete(existing);
    }
}
