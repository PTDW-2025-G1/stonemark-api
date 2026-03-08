package pt.estga.proposal.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.enums.ProposalStatus;
import pt.estga.user.entities.User;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface MarkOccurrenceProposalRepository extends ProposalRepository<MarkOccurrenceProposal> {

    @EntityGraph(attributePaths = {
            "originalMediaFile",
            "existingMonument",
            "existingMark"
    })
    @Override
    Page<MarkOccurrenceProposal> findBySubmittedBy(User user, Pageable pageable);

    @EntityGraph(attributePaths = {
            "submittedBy",
            "originalMediaFile",
            "existingMonument"
    })
    @Query("SELECT p FROM MarkOccurrenceProposal p WHERE " +
           "(:statuses IS NULL OR p.status IN :statuses) AND " +
           "(:submittedById IS NULL OR p.submittedBy.id = :submittedById)")
    Page<MarkOccurrenceProposal> findByFilters(
            @Param("statuses") Collection<ProposalStatus> statuses,
            @Param("submittedById") Long submittedById,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "existingMark",
            "existingMonument",
            "originalMediaFile",
            "submittedBy"
    })
    @Query("SELECT p FROM MarkOccurrenceProposal p WHERE p.id = :id")
    Optional<MarkOccurrenceProposal> findByIdWithRelations(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "existingMark",
            "existingMonument",
            "originalMediaFile",
            "submittedBy"
    })
    @Override
    Optional<MarkOccurrenceProposal> findById(Long id);
}
