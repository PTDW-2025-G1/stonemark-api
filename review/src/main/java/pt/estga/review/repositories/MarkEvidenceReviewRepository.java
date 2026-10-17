package pt.estga.review.repositories;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.shared.jpa.repositories.BaseRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkEvidenceReviewRepository extends BaseRepository<MarkEvidenceReview, UUID> {

    Optional<MarkEvidenceReview> findBySubmissionId(UUID submissionId);

    boolean existsBySubmissionId(UUID submissionId);

    List<MarkEvidenceReview> findByDecision(ReviewDecision decision);
}
