package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.MarkEvidenceRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkEvidenceService {

    private final MarkEvidenceRepository repository;

    public Optional<MarkEvidence> findById(UUID id) {
        return repository.findById(id);
    }

    public MarkEvidence create(MarkEvidence evidence) {
        return repository.save(evidence);
    }

    public MarkEvidence update(MarkEvidence evidence) {
        return repository.save(evidence);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
