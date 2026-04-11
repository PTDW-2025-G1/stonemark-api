package pt.estga.review.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.shared.repositories.BaseRepository;

import java.util.Optional;

@Repository
public interface MarkEvidenceReviewRepository extends BaseRepository<MarkEvidenceReview, Long> {

    Optional<MarkEvidenceReview> findBySubmissionId(Long submissionId);

    boolean existsBySubmissionId(Long submissionId);
}
