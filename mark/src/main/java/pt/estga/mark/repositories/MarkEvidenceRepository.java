package pt.estga.mark.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.estga.mark.entities.MarkEvidence;

import java.util.UUID;

public interface MarkEvidenceRepository extends JpaRepository<MarkEvidence, UUID> {
}
