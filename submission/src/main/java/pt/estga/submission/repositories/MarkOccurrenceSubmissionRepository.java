package pt.estga.submission.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.estga.submission.entities.MarkEvidenceSubmission;
import pt.estga.submission.enums.SubmissionStatus;
import pt.estga.user.entities.User;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface MarkOccurrenceSubmissionRepository extends JpaRepository<MarkEvidenceSubmission, Long>, JpaSpecificationExecutor<MarkEvidenceSubmission> {

    @EntityGraph(attributePaths = {
            "originalMediaFile",
            "existingMonument",
            "existingMark"
    })
    Page<MarkEvidenceSubmission> findBySubmittedBy(User user, Pageable pageable);

    @EntityGraph(attributePaths = {
            "submittedBy",
            "originalMediaFile",
            "existingMonument"
    })
    @Query("SELECT s FROM MarkEvidenceSubmission s WHERE " +
           "(:statuses IS NULL OR s.status IN :statuses) AND " +
           "(:submittedById IS NULL OR s.submittedBy.id = :submittedById)")
    Page<MarkEvidenceSubmission> findByFilters(
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
    @Query("SELECT s FROM MarkEvidenceSubmission s WHERE s.id = :id")
    Optional<MarkEvidenceSubmission> findByIdWithRelations(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "existingMark",
            "existingMonument",
            "originalMediaFile",
            "submittedBy"
    })
    @Override
    Optional<MarkEvidenceSubmission> findById(Long id);

    @Query("""
    SELECT COUNT(s) FROM MarkEvidenceSubmission s
    WHERE s.submittedBy.id = :userId
    AND s.status IN ('AUTO_ACCEPTED', 'MANUALLY_ACCEPTED')
    """)
    long countAcceptedByUserId(@Param("userId") Long userId);
}
