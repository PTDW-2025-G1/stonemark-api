package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.markapi.MarkEvidenceQueryService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkEvidenceQueryServiceImpl implements MarkEvidenceQueryService {

    private final MarkEvidenceRepository evidenceRepository;

    @Override
    public List<MarkEvidenceDistanceDto> findTopKSimilar(String vector, int k, double maxDistance) {
        return evidenceRepository.findTopKSimilarEvidence(vector, k, maxDistance).stream()
                .map(p -> new MarkEvidenceDistanceDto(p.id(), p.getOccurrenceId(), p.getSimilarity()))
                .toList();
    }

    @Override
    public List<MarkEvidenceDistanceDto> findTopKSimilar(String vector, int k) {
        return evidenceRepository.findTopKSimilarEvidence(vector, k).stream()
                .map(p -> new MarkEvidenceDistanceDto(p.id(), p.getOccurrenceId(), p.getSimilarity()))
                .toList();
    }

    @Override
    public List<EvidenceMarkDto> findMarksByEvidenceIds(List<UUID> evidenceIds) {
        return evidenceRepository.findMarksByEvidenceIds(evidenceIds).stream()
                .map(p -> new EvidenceMarkDto(p.getId(), p.getMark().getId()))
                .toList();
    }

    @Override
    public List<MarkEvidenceDto> findEvidenceWithEmbeddings(int limit) {
        var page = PageRequest.of(0, Math.max(1, limit));
        return evidenceRepository.findAllByEmbeddingIsNotNull(page).stream()
                .map(MarkEvidenceMapper::toDto)
                .toList();
    }

}
