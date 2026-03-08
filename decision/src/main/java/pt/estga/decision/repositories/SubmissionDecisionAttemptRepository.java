package pt.estga.decision.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.decision.entities.SubmissionDecisionAttempt;

import java.util.List;
import java.util.Optional;

public interface SubmissionDecisionAttemptRepository extends JpaRepository<SubmissionDecisionAttempt, Long> {

    @EntityGraph(attributePaths = "decidedBy")
    Optional<SubmissionDecisionAttempt> findFirstBySubmissionIdOrderByDecidedAtDesc(Long proposalId);

    @EntityGraph(attributePaths = "decidedBy")
    List<SubmissionDecisionAttempt> findBySubmissionIdOrderByDecidedAtDesc(Long proposalId);
}
