package pt.estga.decision.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.decision.entities.ProposalDecisionAttempt;

import java.util.List;
import java.util.Optional;

public interface ProposalDecisionAttemptRepository extends JpaRepository<ProposalDecisionAttempt, Long> {

    @EntityGraph(attributePaths = "decidedBy")
    Optional<ProposalDecisionAttempt> findFirstByProposalIdOrderByDecidedAtDesc(Long proposalId);

    @EntityGraph(attributePaths = "decidedBy")
    List<ProposalDecisionAttempt> findByProposalIdOrderByDecidedAtDesc(Long proposalId);
}
