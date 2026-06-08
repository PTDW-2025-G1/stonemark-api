package pt.estga.processing.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.services.processing.SuggestionQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/processing/submission")
@RequiredArgsConstructor
@Tag(name = "Processing Suggestions", description = "Suggestions for a processing submission.")
@PreAuthorize("hasAnyRole('REVIEWER', 'MODERATOR', 'ADMIN')")
public class ProcessingSuggestionsController {

    private final SuggestionQueryService suggestionQueryService;

    @GetMapping("/{submissionId}/suggestions")
    public ResponseEntity<List<MarkSuggestionDto>> getSuggestionsForSubmission(@PathVariable Long submissionId) {
        return suggestionQueryService.findSuggestionsBySubmissionId(submissionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
