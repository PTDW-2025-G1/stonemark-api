package pt.estga.monument.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;

@Service
@RequiredArgsConstructor
public class MonumentService {

    private final MonumentRepository repository;

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
