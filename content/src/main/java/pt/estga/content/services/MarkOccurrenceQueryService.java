package pt.estga.content.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.content.entities.MarkOccurrence;
import pt.estga.content.repositories.MarkOccurrenceRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkOccurrenceQueryService {

    private final MarkOccurrenceRepository repository;

    public Page<MarkOccurrence> findAll(Pageable pageable) {
        return repository.findByActiveIsTrue(pageable);
    }

    public Optional<MarkOccurrence> findById(Long id) {
        return repository.findById(id);
    }
}
