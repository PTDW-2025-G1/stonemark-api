package pt.estga.processing.services.draft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.processing.repositories.DraftMarkEvidenceRepository;
import pt.estga.processing.entities.DraftMarkEvidence;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DraftMarkEvidenceQueryService {

    private final DraftMarkEvidenceRepository repository;

    public Optional<DraftMarkEvidence> findBySubmissionId(Long submissionId) {
        return Optional.ofNullable(repository.findBySubmissionId(submissionId));
    }

    public Optional<DraftMarkEvidence> findById(Long id) {
        return repository.findById(id);
    }
}
