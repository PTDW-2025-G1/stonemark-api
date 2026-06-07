package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.review.dtos.ModerationDtos;
import pt.estga.sharedcore.enums.ValidationState;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModerationService {

    private final MarkSuggestionRepository suggestionRepository;
    private final MarkRepository markRepository;

    public List<ModerationDtos.ProcessingDto> findProcessingByConfidence(double min, double max) {
        return suggestionRepository.findProcessingByMaxConfidenceBetween(min, max)
                .stream()
                .map(p -> new ModerationDtos.ProcessingDto(
                        p.getProcessingId(), p.getSubmissionId(), p.getStatus(), p.getMaxConfidence()))
                .collect(Collectors.toList());
    }

    public List<ModerationDtos.MarkDto> findProvisionalMarks() {
        return markRepository.findByValidationState(ValidationState.PROVISIONAL)
                .stream()
                .map(m -> new ModerationDtos.MarkDto(m.getId(), m.getTitle(), m.getValidationState()))
                .collect(Collectors.toList());
    }
}
