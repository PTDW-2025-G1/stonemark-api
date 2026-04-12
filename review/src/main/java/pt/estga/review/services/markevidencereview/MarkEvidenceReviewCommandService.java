package pt.estga.review.services.markevidencereview;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkEvidenceReviewCommandService {

    private final MarkEvidenceReviewRepository repository;

    public MarkEvidenceReview create(MarkEvidenceReview review) {
        return repository.save(review);
    }

    public MarkEvidenceReview update(MarkEvidenceReview review) {
        return repository.save(review);
    }
}
