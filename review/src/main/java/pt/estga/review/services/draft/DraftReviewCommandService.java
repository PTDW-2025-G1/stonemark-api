package pt.estga.review.services.draft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.review.entities.DraftReview;
import pt.estga.review.repositories.DraftReviewRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class DraftReviewCommandService {

    private final DraftReviewRepository repository;

    public DraftReview create(DraftReview review) {
        if (review == null) throw new IllegalArgumentException("Review must not be null");
        return repository.save(review);
    }
}
