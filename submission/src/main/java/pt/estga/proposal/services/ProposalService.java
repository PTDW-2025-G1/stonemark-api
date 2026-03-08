package pt.estga.proposal.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.projections.ProposalStatsProjection;
import pt.estga.user.entities.User;

import java.util.Optional;

public interface ProposalService {

    Page<Proposal> getAll(Pageable pageable);

    Optional<Proposal> findById(Long id);

    Page<Proposal> findByUser(User user, Pageable pageable);

    ProposalStatsProjection getStatsByUser(User user);

    void delete(Long id);
}
