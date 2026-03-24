package pt.estga.submission.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.estga.submission.entities.MarkEvidenceSubmission;

import java.util.Optional;

@Repository
public interface MarkEvidenceSubmissionRepository extends JpaRepository<MarkEvidenceSubmission, Long>, JpaSpecificationExecutor<MarkEvidenceSubmission> {

    @EntityGraph(attributePaths = {
            "originalMediaFile",
            "submittedBy"
    })
    @Override
    Optional<MarkEvidenceSubmission> findById(Long id);

}
