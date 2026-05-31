package pt.estga.review.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.review.dtos.AcceptGroupRequest;
import pt.estga.review.dtos.AcceptNewMarkRequest;
import pt.estga.review.dtos.GroupResponseDto;
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
@PreAuthorize("hasAuthority('REVIEW_MODERATE')")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{submissionId}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Accept an existing mark suggestion")
    public ReviewResponseDto acceptSuggestion(
            @PathVariable Long submissionId,
            @RequestParam Long markId,
            @RequestBody(required = false) SimpleReviewRequest request) {

        var comment = request != null ? request.comment() : null;
        return ReviewMapper.toDto(reviewService.acceptSuggestion(submissionId, markId, comment));
    }

    @PostMapping("/{submissionId}/accept-as-new")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new mark and accept the submission")
    public ReviewResponseDto acceptAsNew(
            @PathVariable Long submissionId,
            @Valid @RequestBody AcceptNewMarkRequest request) {

        return ReviewMapper.toDto(reviewService.acceptAsNew(submissionId, request.markTitle(), request.comment()));
    }

    @PostMapping("/{submissionId}/reject")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reject all suggestions for this submission")
    public ReviewResponseDto rejectAll(
            @PathVariable Long submissionId,
            @RequestBody(required = false) SimpleReviewRequest request) {

        var comment = request != null ? request.comment() : null;
        return ReviewMapper.toDto(reviewService.rejectAll(submissionId, comment));
    }

    @GetMapping("/{submissionId}")
    public ReviewDecision getReviewStatus(@PathVariable Long submissionId) {
        ReviewDecision status = reviewService.getReviewStatus(submissionId);
        if (status == null) throw new pt.estga.sharedweb.exceptions.ResourceNotFoundException("Review not found");
        return status;
    }

    @GetMapping("/{submissionId}/suggestions")
    public List<MarkSuggestionDto> getSuggestions(@PathVariable Long submissionId) {
        return reviewService.getSuggestions(submissionId);
    }

    @PostMapping("/group/{groupId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Accept an entire review group")
    public void acceptGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody AcceptGroupRequest request) {

        reviewService.acceptGroup(groupId, request);
    }

    @PostMapping("/group/{groupId}/reject")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Reject an entire review group")
    public void rejectGroup(
            @PathVariable Long groupId,
            @RequestBody(required = false) SimpleReviewRequest request) {

        var comment = request != null ? request.comment() : null;
        reviewService.rejectGroup(groupId, comment);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get review group details")
    public GroupResponseDto getGroup(@PathVariable Long groupId) {
        return reviewService.getGroupDto(groupId);
    }
}