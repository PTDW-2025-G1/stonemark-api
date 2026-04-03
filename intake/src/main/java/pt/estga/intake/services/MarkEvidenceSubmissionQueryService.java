package pt.estga.intake.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.intake.entities.MarkEvidenceSubmission;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarkEvidenceSubmissionQueryService {

    private final MarkEvidenceSubmissionRepository repository;

    public Optional<MarkEvidenceSubmission> findById(Long id) {
        return repository.findById(id);
    }
}
