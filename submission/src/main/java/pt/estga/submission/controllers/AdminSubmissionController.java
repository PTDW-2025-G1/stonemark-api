package pt.estga.submission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pt.estga.filterutils.models.PagedRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.sharedweb.exceptions.ResourceNotFoundException;
import pt.estga.submission.dtos.ProposalAdminListDto;
import pt.estga.submission.dtos.ProposalWithRelationsDto;
import pt.estga.submission.mappers.MarkOccurrenceSubmissionMapper;
import pt.estga.submission.mappers.SubmissionAdminMapper;
import pt.estga.submission.services.MarkOccurrenceSubmissionQueryService;

@RestController
@RequestMapping("/api/v1/admin/proposals")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REVIEWER')")
@Tag(name = "Submission Administration", description = "Endpoints for proposal administration and read operations.")
public class AdminSubmissionController {

    private final MarkOccurrenceSubmissionQueryService proposalQueryService;
    private final SubmissionAdminMapper submissionAdminMapper;
    private final MarkOccurrenceSubmissionMapper proposalMapper;

    @Operation(summary = "List proposals for moderation",
               description = "Retrieves a paginated list of proposals, optionally filtered by status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proposals retrieved successfully.")
    })
    @PostMapping("/search")
    public ResponseEntity<Page<ProposalAdminListDto>> getAllProposals(@RequestBody PagedRequest request) {
        return ResponseEntity.ok(proposalQueryService.search(request));
    }

    @Operation(summary = "Get full proposal details",
               description = "Retrieves comprehensive information about a proposal, including history and autofill data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submission details retrieved successfully.",
                    content = @Content(schema = @Schema(implementation = ProposalWithRelationsDto.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProposalWithRelationsDto> getProposalDetails(@PathVariable Long id) {
        var proposal = proposalQueryService.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + id));
        return ResponseEntity.ok(proposalMapper.toWithRelationsDto(proposal));
    }
}
