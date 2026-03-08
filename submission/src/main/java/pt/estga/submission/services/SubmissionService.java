package pt.estga.submission.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.projections.ProposalStatsProjection;
import pt.estga.user.entities.User;

import java.util.Optional;

public interface SubmissionService {

    Page<MarkOccurrenceSubmission> getAll(Pageable pageable);

    Optional<MarkOccurrenceSubmission> findById(Long id);

    Page<MarkOccurrenceSubmission> findByUser(User user, Pageable pageable);

    ProposalStatsProjection getStatsByUser(User user);

    void delete(Long id);
}
