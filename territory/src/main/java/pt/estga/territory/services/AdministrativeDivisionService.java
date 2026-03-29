package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.territory.entities.AdministrativeDivision;
import pt.estga.territory.repositories.AdministrativeDivisionRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdministrativeDivisionService {

    private final AdministrativeDivisionRepository repository;

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
