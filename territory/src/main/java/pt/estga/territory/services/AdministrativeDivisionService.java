package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdministrativeDivisionService {

    private final AdministrativeDivisionRepository repository;

    public Page<AdministrativeDivision> search(Specification<AdministrativeDivision> specification, Pageable pageable) {
        return repository.findAll(specification, pageable);
    }

    public Optional<AdministrativeDivision> findById(Long id) {
        return repository.findById(id);
    }

    public AdministrativeDivision create(AdministrativeDivision division) {
        return repository.save(division);
    }

    public AdministrativeDivision update(AdministrativeDivision division) {
        return repository.save(division);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
