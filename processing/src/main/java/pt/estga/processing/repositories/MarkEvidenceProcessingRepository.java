package pt.estga.processing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;

import java.util.UUID;
import java.util.Optional;

public interface MarkEvidenceProcessingRepository extends JpaRepository<MarkEvidenceProcessing, UUID> {

	boolean existsBySubmissionId(Long submissionId);

    Optional<MarkEvidenceProcessing> findBySubmissionId(Long submissionId);
}
