package pt.estga.processing.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/processing/submission")
@RequiredArgsConstructor
@Tag(name = "Processing Suggestions", description = "Suggestions for a processing submission.")
public class ProcessingSuggestionsController {

    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;
    private final MarkSuggestionMapper suggestionMapper;

    @GetMapping("/{submissionId}/suggestions")
    public ResponseEntity<List<MarkSuggestionDto>> getSuggestionsForSubmission(@PathVariable Long submissionId) {
        return processingRepository.findBySubmissionId(submissionId)
                .map(p -> suggestionRepository.findByProcessingId(p.getId())
                        .stream()
                        .map(suggestionMapper::toDto)
                        .toList())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
