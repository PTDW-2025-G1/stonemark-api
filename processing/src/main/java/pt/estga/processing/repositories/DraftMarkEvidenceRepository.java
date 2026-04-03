package pt.estga.processing.repositories;

import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.shared.repositories.BaseRepository;

public interface DraftMarkEvidenceRepository extends BaseRepository<DraftMarkEvidence, Long> {
	DraftMarkEvidence findBySubmissionId(Long submissionId);
}
