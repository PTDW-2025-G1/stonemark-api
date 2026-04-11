package pt.estga.review.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.review.enums.ReviewDecision;
import pt.estga.review.mappers.ReviewMapper;
import pt.estga.review.dtos.ReviewRequestDto;
import pt.estga.review.services.ReviewService;
import pt.estga.review.entities.MarkEvidenceReview;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review submissions and accept/reject suggestions")
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper mapper;

    /**
     * Accept a suggestion for the submission. Expects markId as a request parameter.
     */
    @PostMapping("/{submissionId}/accept")
    public ResponseEntity<?> acceptSuggestion(@PathVariable Long submissionId, @RequestParam Long markId, @RequestBody(required = false) ReviewRequestDto body) {
        try {
            String comment = body == null ? null : body.getComment();
            MarkEvidenceReview review = reviewService.acceptSuggestion(submissionId, markId, comment);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(review));
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ise) {
            // e.g. not processed yet or already reviewed
            log.warn("Conflict while accepting suggestion for submission {}: {}", submissionId, ise.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ise.getMessage());
        }
    }

    /**
     * Reject all suggestions for the submission.
     */
    @PostMapping("/{submissionId}/reject")
    public ResponseEntity<?> rejectAll(@PathVariable Long submissionId, @RequestBody(required = false) ReviewRequestDto body) {
        try {
            String comment = body == null ? null : body.getComment();
            MarkEvidenceReview review = reviewService.rejectAll(submissionId, comment);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(review));
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ise) {
            log.warn("Conflict while rejecting suggestions for submission {}: {}", submissionId, ise.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ise.getMessage());
        }
    }

    /**
     * Get current review decision for a submission.
     */
    @GetMapping("/{submissionId}")
    public ResponseEntity<ReviewDecision> getReviewStatus(@PathVariable Long submissionId) {
        try {
            ReviewDecision decision = reviewService.getReviewStatus(submissionId);
            if (decision == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(decision);
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.notFound().build();
        }
    }
}
