package pt.estga.submission.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.submission.projections.ProposalStatsProjection;
import pt.estga.user.entities.User;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface MarkOccurrenceSubmissionRepository extends JpaRepository<MarkOccurrenceSubmission, Long> {

    @EntityGraph(attributePaths = {
            "originalMediaFile",
            "existingMonument",
            "existingMark"
    })
    Page<MarkOccurrenceSubmission> findBySubmittedBy(User user, Pageable pageable);

    @EntityGraph(attributePaths = {
            "submittedBy",
            "originalMediaFile",
            "existingMonument"
    })
    @Query("SELECT s FROM MarkOccurrenceSubmission s WHERE " +
           "(:statuses IS NULL OR s.status IN :statuses) AND " +
           "(:submittedById IS NULL OR s.submittedBy.id = :submittedById)")
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
    @Query("SELECT s FROM MarkOccurrenceSubmission s WHERE s.id = :id")
    Optional<MarkOccurrenceSubmission> findByIdWithRelations(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "existingMark",
            "existingMonument",
            "originalMediaFile",
            "submittedBy"
    })
    @Override
    Optional<MarkOccurrenceSubmission> findById(Long id);

    @Query("""
    SELECT
        SUM(CASE WHEN s.status IN ('AUTO_ACCEPTED', 'MANUALLY_ACCEPTED') THEN 1 ELSE 0 END) as accepted,
        SUM(CASE WHEN s.status IN ('SUBMITTED', 'UNDER_REVIEW') THEN 1 ELSE 0 END) as underReview,
        SUM(CASE WHEN s.status IN ('AUTO_REJECTED', 'MANUALLY_REJECTED') THEN 1 ELSE 0 END) as rejected
    FROM MarkOccurrenceSubmission s
    WHERE s.submittedBy.id = :userId
    """)
    ProposalStatsProjection getStatsByUserId(@Param("userId") Long userId);
}
