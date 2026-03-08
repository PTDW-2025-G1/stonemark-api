package pt.estga.proposal.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.user.entities.User;

import java.util.Optional;

public interface MarkOccurrenceProposalService {

    // Specific methods for MarkOccurrenceProposal
    Optional<MarkOccurrenceProposal> findByIdWithRelations(Long id);

    MarkOccurrenceProposal create(MarkOccurrenceProposal proposal);

    MarkOccurrenceProposal update(MarkOccurrenceProposal proposal);

    // We can keep these for convenience if they return the specific type,
    // but implementation should delegate to the generic service or repository where appropriate.
    Page<MarkOccurrenceProposal> findByUser(User user, Pageable pageable);
    
    Optional<MarkOccurrenceProposal> findById(Long id);
    
    void delete(MarkOccurrenceProposal proposal);
}
