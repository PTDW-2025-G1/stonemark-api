package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.projections.EvidenceMarkProjection;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;

import java.util.List;
import java.util.UUID;

/**
 * DB boundary: fetches candidate evidence projections and mark mappings.
 */
@Service
@RequiredArgsConstructor
public class CandidateFetcher {

    private final MarkEvidenceRepository evidenceRepository;

    public List<MarkEvidenceDistanceProjection> fetchCandidates(String vector, int safeK, double maxDistance) {
        return evidenceRepository.findTopKSimilarEvidence(vector, safeK, maxDistance);
    }

    public List<EvidenceMarkProjection> fetchMarksByEvidenceIds(List<UUID> ids) {
        return evidenceRepository.findMarksByEvidenceIds(ids);
    }
}
