package pt.estga.review.services.markevidencereview;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkEvidenceReviewQueryService {

    private final MarkEvidenceReviewRepository repository;

    public boolean existsBySubmissionId(Long submissionId) {
        return repository.existsBySubmissionId(submissionId);
    }

    public Optional<MarkEvidenceReview> findBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }
}
