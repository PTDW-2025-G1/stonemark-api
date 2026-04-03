package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.intake.mappers.MarkEvidenceSubmissionMapper;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkEvidenceSubmissionCommandService {

    private final MarkEvidenceSubmissionRepository repository;
    private final MarkEvidenceSubmissionMapper mapper;

    public MarkEvidenceSubmission create(MarkEvidenceSubmission submission) {
        return repository.save(submission);
    }

    public MarkEvidenceSubmission update(MarkEvidenceSubmission submission) {
        if (submission.getId() == null) {
            throw new ResourceNotFoundException("Submission id must not be null for update");
        }

        MarkEvidenceSubmission existing = repository.findById(submission.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkEvidenceSubmission with id " + submission.getId() + " not found"));

        mapper.updateFromSubmission(submission, existing);

        return repository.save(existing);
    }

    public void deleteById(Long id) {
        MarkEvidenceSubmission existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MarkEvidenceSubmission with id " + id + " not found"));

        repository.delete(existing);
    }
}
