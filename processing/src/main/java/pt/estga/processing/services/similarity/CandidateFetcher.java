package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.markapi.MarkService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateFetcher {

    private final MarkService markService;

    public List<MarkEvidenceDistanceDto> fetchCandidates(String vector, int safeK, double maxDistance) {
        return markService.findTopKSimilar(vector, safeK, maxDistance);
    }

    public List<EvidenceMarkDto> fetchMarksByEvidenceIds(List<UUID> ids) {
        return markService.findMarksByEvidenceIds(ids);
    }
}
