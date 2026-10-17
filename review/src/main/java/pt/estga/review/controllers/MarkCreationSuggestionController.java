package pt.estga.review.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.review.dtos.CreateMarkFromSuggestionRequest;
import pt.estga.review.dtos.MarkCreationSuggestionDto;
import pt.estga.review.enums.MarkCreationSuggestionStatus;
import pt.estga.review.services.MarkCreationSuggestionService;

import java.util.UUID;

/**
 * Moderation queue of clusters of unmatched submissions that look like one new
 * mark. Creating a mark here links the cluster's occurrences; no mark is created
 * from a submission directly.
 */
@RestController
@RequestMapping("/api/v1/admin/mark-suggestions")
@RequiredArgsConstructor
@Tag(name = "Mark Creation Suggestions", description = "Suggested new marks from clusters of unmatched submissions.")
@PreAuthorize("hasRole('MODERATOR')")
public class MarkCreationSuggestionController {

    private final MarkCreationSuggestionService service;

    @GetMapping
    @Operation(summary = "List mark-creation suggestions")
    public ResponseEntity<Page<MarkCreationSuggestionDto>> list(
            @RequestParam(defaultValue = "OPEN") MarkCreationSuggestionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }

    @PostMapping("/{id}/create")
    @Operation(summary = "Create a mark from a suggestion and link its occurrences")
    public ResponseEntity<MarkCreationSuggestionDto> create(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMarkFromSuggestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createMark(id, request));
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss a suggestion")
    public ResponseEntity<MarkCreationSuggestionDto> dismiss(@PathVariable UUID id) {
        return ResponseEntity.ok(service.dismiss(id));
    }
}
