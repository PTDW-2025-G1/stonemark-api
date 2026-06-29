package pt.estga.review.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.review.dtos.AcceptNewMarkRequest;
import pt.estga.review.dtos.ReviewResponseDto;
import pt.estga.review.dtos.SimpleReviewRequest;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.mappers.ReviewMapper;
import pt.estga.review.services.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/review")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review submissions and manage mark suggestions")
@PreAuthorize("hasAnyRole('REVIEWER', 'MODERATOR', 'ADMIN')")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{submissionId}/accept")
    @Operation(summary = "Accept an existing mark suggestion")
    public ResponseEntity<ReviewResponseDto> acceptSuggestion(
            @PathVariable Long submissionId,
            @RequestParam Long markId,
            @RequestBody(required = false) SimpleReviewRequest request) {

        var comment = request != null ? request.comment() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewMapper.toDto(reviewService.acceptSuggestion(submissionId, markId, comment)));
    }

    @PostMapping("/{submissionId}/accept-as-new")
    @Operation(summary = "Create a new mark and accept the submission")
    public ResponseEntity<ReviewResponseDto> acceptAsNew(
            @PathVariable Long submissionId,
            @Valid @RequestBody AcceptNewMarkRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewMapper.toDto(reviewService.acceptAsNew(submissionId, request.markTitle(), request.comment())));
    }

    @PostMapping("/{submissionId}/reject")
    @Operation(summary = "Reject all suggestions for this submission")
    public ResponseEntity<ReviewResponseDto> rejectAll(
            @PathVariable Long submissionId,
            @RequestBody(required = false) SimpleReviewRequest request) {

        var comment = request != null ? request.comment() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewMapper.toDto(reviewService.rejectAll(submissionId, comment)));
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<ReviewDecision> getReviewStatus(@PathVariable Long submissionId) {
        ReviewDecision status = reviewService.getReviewStatus(submissionId);
        if (status == null) throw new pt.estga.commonweb.exceptions.ResourceNotFoundException("Review not found");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{submissionId}/suggestions")
    public ResponseEntity<List<MarkSuggestionDto>> getSuggestions(@PathVariable Long submissionId) {
        return ResponseEntity.ok(reviewService.getSuggestions(submissionId));
    }
}
