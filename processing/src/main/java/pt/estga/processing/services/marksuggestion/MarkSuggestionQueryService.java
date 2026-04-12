package pt.estga.processing.services.marksuggestion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.processing.repositories.projections.ProcessingModerationProjection;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkSuggestionQueryService {

    private final MarkSuggestionRepository suggestionRepository;

    public List<ProcessingModerationProjection> findByConfidenceRange(double min, double max) {
        return suggestionRepository.findProcessingByMaxConfidenceBetween(min, max);
    }
}
