package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.MarkCategory;
import pt.estga.mark.mappers.MarkCategoryMapper;
import pt.estga.mark.repositories.MarkCategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkCategoryService {

    private final MarkCategoryRepository repository;
    private final MarkCategoryMapper mapper;

    public MarkCategory create(MarkCategory category) {
        return repository.save(category);
    }

    public MarkCategory update(MarkCategory category) {
        if (category.getId() == null) {
            throw new IllegalArgumentException("Category id must not be null for update");
        }

        MarkCategory existing = repository.findById(category.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "MarkCategory with id " + category.getId() + " not found"));

        mapper.update(category, existing);

        return repository.save(existing);
    }

    public void deleteById(Long id) {
        MarkCategory category = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MarkCategory with id " + id + " not found"));

        repository.softDelete(category);
    }
}
