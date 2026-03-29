package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.MarkCategoryRequestDto;
import pt.estga.mark.entities.MarkCategory;
import pt.estga.mark.mappers.MarkCategoryMapper;
import pt.estga.mark.repositories.MarkCategoryRepository;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkCategoryCommandService {

    private final MarkCategoryRepository repository;
    private final MarkCategoryMapper mapper;

    public MarkCategory create(MarkCategory category) {
        return repository.save(category);
    }

    public MarkCategory create(MarkCategoryRequestDto dto) {
        MarkCategory category = mapper.toEntity(dto);

        if (dto.parentId() != null) {
            MarkCategory parent = repository.findById(dto.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParentCategory(parent);
        }

        return repository.save(category);
    }

    public MarkCategory update(MarkCategory category) {
        if (category.getId() == null) {
            throw new ResourceNotFoundException("Category id must not be null for update");
        }

        MarkCategory existing = repository.findById(category.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkCategory with id " + category.getId() + " not found"));

        mapper.update(category, existing);

        return repository.save(existing);
    }

    public MarkCategory update(Long id, MarkCategoryRequestDto dto) {
        MarkCategory existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkCategory with id " + id + " not found"));

        mapper.updateFromDto(dto, existing);

        if (dto.parentId() != null) {
            MarkCategory parent = repository.findById(dto.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            existing.setParentCategory(parent);
        } else {
            existing.setParentCategory(null);
        }

        return repository.save(existing);
    }

    public void deleteById(Long id) {
        MarkCategory category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkCategory with id " + id + " not found"));

        repository.softDelete(category);
    }
}
