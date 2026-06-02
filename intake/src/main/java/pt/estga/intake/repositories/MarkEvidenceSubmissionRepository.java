package pt.estga.intake.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.estga.intake.entities.MarkEvidenceSubmission;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarkEvidenceSubmissionRepository extends JpaRepository<MarkEvidenceSubmission, Long>, JpaSpecificationExecutor<MarkEvidenceSubmission> {

    List<MarkEvidenceSubmission> findByOriginalMediaFileIdIn(List<UUID> originalMediaFileIds);
}
