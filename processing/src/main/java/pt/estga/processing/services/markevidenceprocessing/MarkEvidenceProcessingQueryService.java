package pt.estga.processing.services.markevidenceprocessing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.projections.ProcessingOverviewProjection;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarkEvidenceProcessingQueryService {

    private final MarkEvidenceProcessingRepository repository;

    public Optional<ProcessingOverviewProjection> findOverviewBySubmissionId(Long submissionId) {
        return repository.findOverviewBySubmissionId(submissionId);
    }

    public boolean existsBySubmissionId(Long submissionId) {
        return repository.existsBySubmissionId(submissionId);
    }
}
