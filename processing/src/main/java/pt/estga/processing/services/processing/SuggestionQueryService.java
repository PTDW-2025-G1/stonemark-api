package pt.estga.processing.services.processing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuggestionQueryService {

    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;
    private final MarkSuggestionMapper suggestionMapper;

    public Optional<List<MarkSuggestionDto>> findSuggestionsBySubmissionId(Long submissionId) {
        return processingRepository.findBySubmissionId(submissionId)
                .map(p -> suggestionRepository.findByProcessingId(p.getId())
                        .stream()
                        .map(suggestionMapper::toDto)
                        .toList());
    }
}
