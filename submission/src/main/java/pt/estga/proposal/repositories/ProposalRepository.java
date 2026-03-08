package pt.estga.proposal.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.projections.ProposalStatsProjection;
import pt.estga.user.entities.User;

@Repository
public interface ProposalRepository<T extends Proposal> extends JpaRepository<T, Long> {

    Page<T> findBySubmittedBy(User user, Pageable pageable);

    @Query("""
    SELECT
        SUM(CASE WHEN p.status IN ('AUTO_ACCEPTED', 'MANUALLY_ACCEPTED') THEN 1 ELSE 0 END) as accepted,
        SUM(CASE WHEN p.status IN ('SUBMITTED', 'UNDER_REVIEW') THEN 1 ELSE 0 END) as underReview,
        SUM(CASE WHEN p.status IN ('AUTO_REJECTED', 'MANUALLY_REJECTED') THEN 1 ELSE 0 END) as rejected
    FROM Proposal p
    WHERE p.submittedBy.id = :userId
    """)
    ProposalStatsProjection getStatsByUserId(@Param("userId") Long userId);
}
