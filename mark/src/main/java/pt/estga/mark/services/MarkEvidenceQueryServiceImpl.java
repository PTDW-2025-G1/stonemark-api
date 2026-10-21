package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.mark.entities.MarkEvidence;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.api.MarkEvidenceQueryService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkEvidenceQueryServiceImpl implements MarkEvidenceQueryService {

    private final MarkEvidenceRepository evidenceRepository;

    @Override
    public List<MarkEvidenceDistanceDto> findTopKSimilar(float[] embedding, int k, double minSimilarity) {
        if (embedding == null || embedding.length == 0 || k < 1) {
            return List.of();
        }
        SearchResults<MarkEvidence> results = evidenceRepository
                .findByEmbeddingNearAndOccurrenceIsNotNullAndDeletedAtIsNull(
                        Vector.of(embedding),
                        Score.of(1.0 - minSimilarity, ScoringFunction.cosine()),
                        Limit.of(k));
        return results.getContent().stream()
                .map(result -> new MarkEvidenceDistanceDto(
                        result.getContent().getId(),
                        result.getContent().getOccurrence().getId(),
                        1.0 - result.getScore().getValue()))
                .toList();
    }

    @Override
    public List<EvidenceMarkDto> findMarksByEvidenceIds(List<UUID> evidenceIds) {
        return evidenceRepository.findMarksByEvidenceIds(evidenceIds).stream()
                .map(p -> new EvidenceMarkDto(p.getId(), p.getMark().getId()))
                .toList();
    }
}
