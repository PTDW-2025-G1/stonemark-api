package pt.estga.content.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.repositories.MarkRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkQueryService {

    private final MarkRepository repository;

    public Page<Mark> findAll(Pageable pageable) {
        return repository.findByActiveIsTrue(pageable);
    }

    public Page<Mark> findAllManagement(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<Mark> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Mark> findWithCoverById(Long id) {
        return repository.findWithCoverById(id);
    }

    public long count() {
        return repository.count();
    }
}
