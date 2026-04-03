package pt.estga.processing.services.draft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.processing.repositories.DraftMarkEvidenceRepository;
import pt.estga.processing.entities.DraftMarkEvidence;

@Service
@RequiredArgsConstructor
public class DraftMarkEvidenceCommandService {

    private final DraftMarkEvidenceRepository repository;

    public DraftMarkEvidence create(DraftMarkEvidence draft) {
        if (draft == null) throw new IllegalArgumentException("Draft must not be null");
        return repository.save(draft);
    }

    public DraftMarkEvidence createIfMissingForSubmission(DraftMarkEvidence draft) {
        if (draft == null || draft.getSubmission() == null || draft.getSubmission().getId() == null) {
            throw new IllegalArgumentException("Draft and linked submission id must be provided");
        }

        DraftMarkEvidence existing = repository.findBySubmissionId(draft.getSubmission().getId());
        if (existing != null) return existing;

        return repository.save(draft);
    }
}
