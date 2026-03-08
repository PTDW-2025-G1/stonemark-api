package pt.estga.submission.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.projections.ProposalStatsProjection;
import pt.estga.user.entities.User;

import java.util.Optional;

public interface SubmissionService {

    Page<Submission> getAll(Pageable pageable);

    Optional<Submission> findById(Long id);

    Page<Submission> findByUser(User user, Pageable pageable);

    ProposalStatsProjection getStatsByUser(User user);

    void delete(Long id);
}
