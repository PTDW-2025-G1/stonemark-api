package pt.estga.markapi;

import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.mark.dtos.MarkEvidenceDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarkEvidenceQueryService {

    List<MarkEvidenceDistanceDto> findTopKSimilar(String vector, int k, double maxDistance);

    List<MarkEvidenceDistanceDto> findTopKSimilar(String vector, int k);

    List<EvidenceMarkDto> findMarksByEvidenceIds(List<UUID> evidenceIds);

    List<MarkEvidenceDto> findEvidenceWithEmbeddings(int limit);
}
