package pt.estga.submission.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.user.entities.User;

import java.util.Optional;

public interface MarkOccurrenceProposalService {

    // Specific methods for MarkOccurrenceSubmission
    Optional<MarkOccurrenceSubmission> findByIdWithRelations(Long id);

    MarkOccurrenceSubmission create(MarkOccurrenceSubmission proposal);

    MarkOccurrenceSubmission update(MarkOccurrenceSubmission proposal);

    // We can keep these for convenience if they return the specific type,
    // but implementation should delegate to the generic service or repository where appropriate.
    Page<MarkOccurrenceSubmission> findByUser(User user, Pageable pageable);
    
    Optional<MarkOccurrenceSubmission> findById(Long id);
    
    void delete(MarkOccurrenceSubmission proposal);
}
