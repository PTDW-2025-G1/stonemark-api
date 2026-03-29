package pt.estga.mark.services.evidence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.MarkEvidenceRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkEvidenceQueryService {

    private final MarkEvidenceRepository repository;

    public Optional<MarkEvidence> findById(UUID id) {
        return repository.findById(id);
    }
}
