package pt.estga.processing.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/processing/submission")
@RequiredArgsConstructor
@Tag(name = "Processing Suggestions", description = "Suggestions for a processing submission.")
public class ProcessingSuggestionsController {

    private final MarkSuggestionQueryService queryService;

    @GetMapping("/{submissionId}/suggestions")
    public ResponseEntity<List<MarkSuggestionDto>> getSuggestionsForSubmission(@PathVariable Long submissionId) {
        Optional<List<MarkSuggestionDto>> opt = queryService.findBySubmissionId(submissionId);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
