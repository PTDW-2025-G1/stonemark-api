package pt.estga.review.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkEvidenceReviewRepository extends BaseRepository<MarkEvidenceReview, Long> {

    Optional<MarkEvidenceReview> findBySubmissionId(Long submissionId);

    boolean existsBySubmissionId(Long submissionId);

    List<MarkEvidenceReview> findByDecision(ReviewDecision decision);
}
