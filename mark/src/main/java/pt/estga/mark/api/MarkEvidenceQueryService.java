package pt.estga.mark.api;

import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;

import java.util.List;
import java.util.UUID;

public interface MarkEvidenceQueryService {

    List<MarkEvidenceDistanceDto> findTopKSimilar(String vector, int k, double maxDistance);

    List<EvidenceMarkDto> findMarksByEvidenceIds(List<UUID> evidenceIds);
}
