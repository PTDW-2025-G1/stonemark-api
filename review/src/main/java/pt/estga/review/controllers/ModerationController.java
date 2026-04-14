package pt.estga.review.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.processing.repositories.projections.ProcessingModerationProjection;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.monument.MonumentRepository;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;
import pt.estga.review.dtos.ModerationDtos;
import pt.estga.shared.enums.ValidationState;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final MarkSuggestionQueryService markSuggestionQueryService;
    private final MarkRepository markRepository;
    private final MonumentRepository monumentRepository;

    @GetMapping("/processing-by-confidence")
    public List<ModerationDtos.ProcessingDto> processingByConfidence(
            @RequestParam double min,
            @RequestParam double max) {
        List<ProcessingModerationProjection> result = markSuggestionQueryService.findByConfidenceRange(min, max);
        return result.stream().map(p -> new ModerationDtos.ProcessingDto(
                p.getProcessingId(), p.getSubmissionId(), p.getStatus(), p.getMaxConfidence()
        )).collect(Collectors.toList());
    }

    @GetMapping("/discovery/marks")
    public List<ModerationDtos.MarkDto> provisionalMarks() {
        return markRepository.findByValidationState(ValidationState.PROVISIONAL)
                .stream()
                .map(m -> new ModerationDtos.MarkDto(m.getId(), m.getTitle(), m.getValidationState()))
                .collect(Collectors.toList());
    }

    @GetMapping("/discovery/monuments")
    public List<ModerationDtos.MonumentDto> provisionalMonuments() {
        return monumentRepository.findByValidationState(ValidationState.PROVISIONAL)
                .stream()
                .map(m -> new ModerationDtos.MonumentDto(m.getId(), m.getName(), m.getValidationState()))
                .collect(Collectors.toList());
    }
}
