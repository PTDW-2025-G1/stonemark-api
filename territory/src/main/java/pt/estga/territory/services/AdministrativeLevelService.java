package pt.estga.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.territory.entities.AdministrativeLevel;
import pt.estga.territory.repositories.AdministrativeLevelRepository;

@Service
@RequiredArgsConstructor
public class AdministrativeLevelService {

    private final AdministrativeLevelRepository repository;

    public AdministrativeLevel create(AdministrativeLevel level) {
        return repository.save(level);
    }

    public void deleteById(Integer id) {
        repository.findById(id).ifPresent(repository::softDelete);
    }
}
