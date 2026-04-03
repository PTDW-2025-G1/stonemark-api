package pt.estga.processing.services.draft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.processing.repositories.DraftMarkEvidenceRepository;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.mappers.DraftMarkEvidenceMapper;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class DraftMarkEvidenceCommandService {

    private final DraftMarkEvidenceRepository repository;
    private final DraftMarkEvidenceMapper mapper;

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

    public DraftMarkEvidence update(DraftMarkEvidence draft) {
        if (draft == null || draft.getId() == null) {
            throw new IllegalArgumentException("Draft id must not be null for update");
        }

        DraftMarkEvidence existing = repository.findById(draft.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Draft with id " + draft.getId() + " not found"));

        mapper.updateFromDraft(draft, existing);

        return repository.save(existing);
    }
}
