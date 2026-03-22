package pt.estga.content.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Monument;
import pt.estga.content.repositories.MonumentRepository;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.services.AdministrativeDivisionQueryService;
import pt.estga.territory.services.AdministrativeDivisionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonumentService {

    private final MonumentRepository repository;

    public Optional<Monument> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Monument create(Monument monument) {
        return repository.save(monument);
    }

    @Transactional
    public Monument update(Monument monument) {
        return repository.save(monument);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.findById(id).ifPresent(monument -> {
            repository.deleteById(id);
        });
    }
}
