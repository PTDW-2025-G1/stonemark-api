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

    /**
     * Load draft with a pessimistic write lock to ensure safe concurrent review processing.
     */
    public Optional<DraftMarkEvidence> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id);
    }

    /**
     * Check whether the draft for the given submission id is ready for review.
     */
    public boolean isDraftReadyForReview(Long submissionId) {
        return findBySubmissionId(submissionId)
                .map(DraftMarkEvidence::isReadyForReview)
                .orElse(false);
    }
}
