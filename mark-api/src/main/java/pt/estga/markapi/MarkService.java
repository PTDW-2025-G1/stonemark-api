package pt.estga.markapi;

import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarkService {

    Optional<MarkDto> findMarkById(Long id);

    Optional<MarkOccurrenceDto> findOccurrenceById(Long id);

    Optional<MarkEvidenceDto> findEvidenceById(UUID id);

    List<MarkEvidenceDistanceDto> findTopKSimilar(String vector, int k, double maxDistance);

    List<MarkEvidenceDistanceDto> findTopKSimilar(String vector, int k);

    List<EvidenceMarkDto> findMarksByEvidenceIds(List<UUID> evidenceIds);

    List<MarkEvidenceDto> findEvidenceWithEmbeddings(int limit);

    List<MarkEvidenceDto> findEvidenceByIdIn(List<UUID> ids);
}
