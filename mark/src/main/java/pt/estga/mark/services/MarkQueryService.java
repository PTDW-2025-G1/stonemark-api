package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkQueryService {

    private final MarkRepository repository;

    public Optional<Mark> findById(Long id) {
        return repository.findById(id);
    }
}
