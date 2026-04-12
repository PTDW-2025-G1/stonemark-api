package pt.estga.processing.services.suggestions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkSuggestionQueryService {

    private final MarkSuggestionRepository markSuggestionRepository;
    private final MarkSuggestionMapper mapper;
    private final MarkEvidenceProcessingRepository processingRepository;

    public Optional<MarkSuggestion> findById(UUID id) {
        return markSuggestionRepository.findById(id);
    }

    public Optional<MarkSuggestionDto> findDtoById(UUID id) {
        return findById(id).map(mapper::toDto);
    }

    public Optional<List<MarkSuggestionDto>> findBySubmissionId(Long submissionId) {
        return processingRepository.findBySubmissionId(submissionId)
                .map(p -> markSuggestionRepository
                                .findByProcessingId(p.getId())
                                .stream().map(mapper::toDto)
                                .toList());
    }

    public boolean existsByProcessingIdAndMarkId(UUID processingId, Long markId) {
        return markSuggestionRepository.existsByProcessingIdAndMarkId(processingId, markId);
    }

    public long countByProcessingId(UUID processingId) {
        return markSuggestionRepository.countByProcessingId(processingId);
    }

    public Double findMaxConfidenceByProcessingId(UUID processingId) {
        return markSuggestionRepository.findMaxConfidenceByProcessingId(processingId);
    }

    public Optional<MarkSuggestion> findByProcessingIdAndMarkId(UUID processingId, Long markId) {
        return markSuggestionRepository.findByProcessingIdAndMarkId(processingId, markId);
    }
}
