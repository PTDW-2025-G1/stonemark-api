package pt.estga.submission.repositories;

import org.springframework.stereotype.Repository;
import pt.estga.submission.entities.MarkSubmission;

@Repository
public interface MarkProposalRepository extends ProposalRepository<MarkSubmission> {
}
