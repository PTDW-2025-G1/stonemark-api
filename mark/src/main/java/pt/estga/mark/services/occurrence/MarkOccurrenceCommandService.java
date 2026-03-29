package pt.estga.mark.services.occurrence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.repositories.MarkOccurrenceRepository;

@Service
@RequiredArgsConstructor
public class MarkOccurrenceCommandService {

    private final MarkOccurrenceRepository repository;

    @Transactional
    public MarkOccurrence create(MarkOccurrence occurrence) {
        return repository.save(occurrence);
    }

    @Transactional
    public MarkOccurrence update(MarkOccurrence occurrence) {
        return repository.save(occurrence);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
