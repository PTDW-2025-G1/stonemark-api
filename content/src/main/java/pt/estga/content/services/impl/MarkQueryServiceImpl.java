package pt.estga.content.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.Mark;
import pt.estga.content.repositories.MarkRepository;
import pt.estga.content.services.MarkQueryService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkQueryServiceImpl implements MarkQueryService {

    private final MarkRepository repository;

    @Override
    public Page<Mark> findAll(Pageable pageable) {
        return repository.findByActiveIsTrue(pageable);
    }

    @Override
    public Page<Mark> findAllManagement(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<Mark> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Mark> findWithCoverById(Long id) {
        return repository.findWithCoverById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
