package pt.estga.review.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.estga.review.dtos.AcceptNewMarkRequest;
import pt.estga.review.dtos.ReviewResponseDto;
import pt.estga.review.dtos.SimpleReviewRequest;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.mappers.ReviewMapper;
import pt.estga.review.services.ReviewService;
import pt.estga.processing.services.suggestions.MarkSuggestionQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/review")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review submissions and manage mark suggestions")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper mapper;
    private final MarkSuggestionQueryService suggestionQueryService;

    @PostMapping("/{submissionId}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Accept an existing mark suggestion")
    public ReviewResponseDto acceptSuggestion(
            @PathVariable Long submissionId,
            @RequestParam Long markId,
            @RequestBody(required = false) SimpleReviewRequest request) {

        var comment = request != null ? request.comment() : null;
        return mapper.toDto(reviewService.acceptSuggestion(submissionId, markId, comment));
    }

    @PostMapping("/{submissionId}/accept-as-new")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new mark and accept the submission")
    public ReviewResponseDto acceptAsNew(
            @PathVariable Long submissionId,
            @Valid @RequestBody AcceptNewMarkRequest request) {

        return mapper.toDto(reviewService.acceptAsNew(submissionId, request.markTitle(), request.comment()));
    }

    @PostMapping("/{submissionId}/reject")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reject all suggestions for this submission")
    public ReviewResponseDto rejectAll(
            @PathVariable Long submissionId,
            @RequestBody(required = false) SimpleReviewRequest request) {

        var comment = request != null ? request.comment() : null;
        return mapper.toDto(reviewService.rejectAll(submissionId, comment));
    }

    @GetMapping("/{submissionId}")
    public ReviewDecision getReviewStatus(@PathVariable Long submissionId) {
        ReviewDecision status = reviewService.getReviewStatus(submissionId);
        if (status == null) throw new pt.estga.sharedweb.exceptions.ResourceNotFoundException("Review not found");
        return status;
    }

    @GetMapping("/{submissionId}/suggestions")
    public List<?> getSuggestions(@PathVariable Long submissionId) {
        // Assuming findBySubmissionId returns an Optional<List> or similar
        return suggestionQueryService.findBySubmissionId(submissionId)
                .orElseThrow(() -> new pt.estga.sharedweb.exceptions.ResourceNotFoundException("Suggestions not found"));
    }
}