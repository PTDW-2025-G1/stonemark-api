package pt.estga.mark.services.occurrence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.repositories.MarkOccurrenceRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkOccurrenceQueryService {

    private final MarkOccurrenceRepository repository;

    public Optional<MarkOccurrence> findById(Long id) {
        return repository.findById(id);
    }
}
