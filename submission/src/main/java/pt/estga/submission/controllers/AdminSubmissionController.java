package pt.estga.submission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.submission.dtos.ProposalAdminListDto;
import pt.estga.submission.dtos.ProposalFilter;
import pt.estga.submission.dtos.ProposalWithRelationsDto;
import pt.estga.submission.mappers.SubmissionAdminMapper;
import pt.estga.submission.repositories.MarkOccurrenceSubmissionRepository;
import pt.estga.submission.services.SubmissionQueryService;

@RestController
@RequestMapping("/api/v1/admin/proposals")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REVIEWER')")
@Tag(name = "Submission Administration", description = "Endpoints for proposal administration and read operations.")
public class AdminSubmissionController {

    private final MarkOccurrenceSubmissionRepository proposalRepo;
    private final SubmissionAdminMapper submissionAdminMapper;
    private final SubmissionQueryService submissionQueryService;

    @Operation(summary = "List proposals for moderation",
               description = "Retrieves a paginated list of proposals, optionally filtered by status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proposals retrieved successfully.")
    })
    @GetMapping
    public ResponseEntity<Page<ProposalAdminListDto>> getAllProposals(
            @ParameterObject ProposalFilter filter,
            @ParameterObject @PageableDefault(sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var statuses = filter.statuses();
        if (statuses != null && statuses.isEmpty()) {
            statuses = null;
        }

        return ResponseEntity.ok(proposalRepo.findByFilters(statuses, filter.submittedById(), pageable)
                .map(submissionAdminMapper::toAdminListDto));
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
        return ResponseEntity.ok(submissionQueryService.getProposalDetails(id));
    }
}
