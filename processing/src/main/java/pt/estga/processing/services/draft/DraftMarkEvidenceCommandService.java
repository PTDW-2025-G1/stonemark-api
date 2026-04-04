package pt.estga.processing.services.draft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.processing.repositories.DraftMarkEvidenceRepository;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.mappers.DraftMarkEvidenceMapper;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class DraftMarkEvidenceCommandService {

    private final DraftMarkEvidenceRepository repository;
    private final DraftMarkEvidenceMapper mapper;

    public DraftMarkEvidence create(DraftMarkEvidence draft) {
        if (draft == null) throw new IllegalArgumentException("Draft must not be null");
        // Ensure newly created drafts have sensible defaults.
        if (draft.getActive() == null) draft.setActive(true);
        if (draft.getVersion() == null) draft.setVersion(1);
        if (draft.getProcessingStatus() == null) draft.setProcessingStatus(ProcessingStatus.PENDING);

        return repository.save(draft);
    }

    public DraftMarkEvidence createIfMissingForSubmission(DraftMarkEvidence draft) {
        if (draft == null || draft.getSubmission() == null || draft.getSubmission().getId() == null) {
            throw new IllegalArgumentException("Draft and linked submission id must be provided");
        }

        DraftMarkEvidence existing = repository.findBySubmissionId(draft.getSubmission().getId());
        // If an active draft already exists for the submission, reuse it to avoid duplicates.
        if (existing != null && Boolean.TRUE.equals(existing.getActive())) return existing;

        // Ensure sensible defaults for a freshly created draft.
        if (draft.getActive() == null) draft.setActive(true);
        if (draft.getVersion() == null) draft.setVersion(1);
        if (draft.getProcessingStatus() == null) draft.setProcessingStatus(ProcessingStatus.PENDING);

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
