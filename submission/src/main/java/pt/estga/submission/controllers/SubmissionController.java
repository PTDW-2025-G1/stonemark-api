package pt.estga.submission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.submission.dtos.ProposalSummaryDto;
import pt.estga.submission.mappers.SubmissionMapper;
import pt.estga.submission.services.MarkOccurrenceSubmissionQueryService;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.entities.User;

@RestController
@RequestMapping("/api/v1/public/proposals")
@RequiredArgsConstructor
@Tag(name = "Proposals (Generic)", description = "Generic endpoints for listing and querying all types of proposals.")
public class SubmissionController {

    private final MarkOccurrenceSubmissionQueryService submissionQueryService;
    private final SubmissionMapper submissionMapper;

    @Operation(summary = "List all proposals by user",
            description = "Retrieves a paginated list of all proposals (any type) submitted by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proposals retrieved successfully.")
    })
    @GetMapping("/user/me")
    public Page<ProposalSummaryDto> findByUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        User user = User.builder().id(principal.getId()).build();
        return submissionQueryService.findBySubmittedBy(PageRequest.of(page, size), user)
                .map(submissionMapper::toSummaryDto);
    }

    @Operation(summary = "Get proposal summary by ID",
            description = "Retrieves a basic summary of a proposal by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submission found.",
                    content = @Content(schema = @Schema(implementation = ProposalSummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProposalSummaryDto> findById(@PathVariable Long id) {
        return submissionQueryService.findById(id)
                .map(submissionMapper::toSummaryDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
