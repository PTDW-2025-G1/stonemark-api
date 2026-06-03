package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.markapi.MarkEvidenceQueryService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateFetcher {

    private final MarkEvidenceQueryService markEvidenceQueryService;

    public List<MarkEvidenceDistanceDto> fetchCandidates(String vector, int safeK, double maxDistance) {
        return markEvidenceQueryService.findTopKSimilar(vector, safeK, maxDistance);
    }

    public List<EvidenceMarkDto> fetchMarksByEvidenceIds(List<UUID> ids) {
        return markEvidenceQueryService.findMarksByEvidenceIds(ids);
    }
}
