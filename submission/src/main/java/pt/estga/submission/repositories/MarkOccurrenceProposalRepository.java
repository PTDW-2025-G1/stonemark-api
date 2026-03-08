package pt.estga.submission.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.user.entities.User;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface MarkOccurrenceProposalRepository extends ProposalRepository<MarkOccurrenceSubmission> {

    @EntityGraph(attributePaths = {
            "originalMediaFile",
            "existingMonument",
            "existingMark"
    })
    @Override
    Page<MarkOccurrenceSubmission> findBySubmittedBy(User user, Pageable pageable);

    @EntityGraph(attributePaths = {
            "submittedBy",
            "originalMediaFile",
            "existingMonument"
    })
    @Query("SELECT p FROM MarkOccurrenceSubmission p WHERE " +
           "(:statuses IS NULL OR p.status IN :statuses) AND " +
           "(:submittedById IS NULL OR p.submittedBy.id = :submittedById)")
    Page<MarkOccurrenceSubmission> findByFilters(
            @Param("statuses") Collection<SubmissionStatus> statuses,
            @Param("submittedById") Long submittedById,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "existingMark",
            "existingMonument",
            "originalMediaFile",
            "submittedBy"
    })
    @Query("SELECT p FROM MarkOccurrenceSubmission p WHERE p.id = :id")
    Optional<MarkOccurrenceSubmission> findByIdWithRelations(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "existingMark",
            "existingMonument",
            "originalMediaFile",
            "submittedBy"
    })
    @Override
    Optional<MarkOccurrenceSubmission> findById(Long id);
}
