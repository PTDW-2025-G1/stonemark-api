package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import pt.estga.mark.dtos.EvidenceMarkDto;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.dtos.MarkOccurrenceDto;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.mappers.MarkMapper;
import pt.estga.mark.mappers.MarkOccurrenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.MarkOccurrenceRepository;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.markapi.MarkService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarkServiceImpl implements MarkService {

    private final MarkRepository markRepository;
    private final MarkOccurrenceRepository occurrenceRepository;
    private final MarkEvidenceRepository evidenceRepository;

    @Override
    public Optional<MarkDto> findMarkById(Long id) {
        return markRepository.findById(id).map(MarkMapper::toDto);
    }

    @Override
    public Optional<MarkOccurrenceDto> findOccurrenceById(Long id) {
        return occurrenceRepository.findById(id).map(MarkOccurrenceMapper::toDto);
    }

    @Override
    public Optional<MarkEvidenceDto> findEvidenceById(UUID id) {
        return evidenceRepository.findById(id).map(MarkEvidenceMapper::toDto);
    }

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

    @Override
    public List<MarkEvidenceDto> findEvidenceByIdIn(List<UUID> ids) {
        return evidenceRepository.findAllById(ids).stream()
                .map(MarkEvidenceMapper::toDto)
                .toList();
    }
}
