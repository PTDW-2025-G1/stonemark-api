package pt.estga.intake.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.estga.intake.entities.MarkEvidenceSubmission;

import java.util.Optional;

@Repository
public interface MarkEvidenceSubmissionRepository extends JpaRepository<MarkEvidenceSubmission, UUID>, JpaSpecificationExecutor<MarkEvidenceSubmission> {

    Optional<MarkEvidenceSubmission> findByIdempotencyKey(String idempotencyKey);
}
