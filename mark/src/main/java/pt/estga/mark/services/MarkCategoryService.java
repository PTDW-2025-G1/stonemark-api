package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.MarkCategory;
import pt.estga.mark.repositories.MarkCategoryRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarkCategoryService {

    private final MarkCategoryRepository repository;

    public MarkCategory create(MarkCategory category) {
        return repository.save(category);
    }

    public MarkCategory update(MarkCategory category) {
        return repository.save(category);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
