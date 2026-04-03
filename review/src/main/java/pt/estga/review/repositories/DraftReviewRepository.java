package pt.estga.review.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.review.entities.DraftReview;
import pt.estga.shared.repositories.BaseRepository;

import java.util.Optional;

@Repository
public interface DraftReviewRepository extends BaseRepository<DraftReview, Long> {

    boolean existsByDraftId(Long draftId);

    Optional<DraftReview> findByDraftId(Long draftId);
}
