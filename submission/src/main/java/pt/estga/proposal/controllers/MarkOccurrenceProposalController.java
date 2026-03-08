package pt.estga.proposal.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.proposal.dtos.MarkOccurrenceProposalCreateDto;
import pt.estga.proposal.dtos.MarkOccurrenceProposalDto;
import pt.estga.proposal.dtos.MarkOccurrenceProposalListDto;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.mappers.MarkOccurrenceProposalMapper;
import pt.estga.proposal.services.MarkOccurrenceProposalService;
import pt.estga.proposal.services.submission.MarkOccurrenceProposalSubmissionService;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.entities.User;

@RestController
@RequestMapping("/api/v1/public/proposals/mark-occurrences")
@RequiredArgsConstructor
@Tag(name = "Mark Occurrence Proposals", description = "Endpoints for querying, retrieving, and submitting mark occurrence proposals.")
public class MarkOccurrenceProposalController {

    private final MarkOccurrenceProposalService proposalService;
    private final MarkOccurrenceProposalSubmissionService submissionService;
    private final MarkOccurrenceProposalMapper markOccurrenceProposalMapper;

    @Operation(summary = "List proposals by user",
            description = "Retrieves a paginated list of mark occurrence proposals submitted by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proposals retrieved successfully.")
    })
    @GetMapping("/user/me")
    public Page<MarkOccurrenceProposalListDto> findByUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        User user = User.builder().id(principal.getId()).build();
        return proposalService.findByUser(user, PageRequest.of(page, size))
                .map(markOccurrenceProposalMapper::toListDto);
    }

    @Operation(summary = "List detailed proposals by user",
            description = "Retrieves a paginated list of detailed mark occurrence proposals submitted by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detailed proposals retrieved successfully.")
    })
    @GetMapping("/user/me/detailed")
    public Page<MarkOccurrenceProposalDto> findDetailedByUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        User user = User.builder().id(principal.getId()).build();
        return proposalService.findByUser(user, PageRequest.of(page, size))
                .map(markOccurrenceProposalMapper::toDto);
    }

    @Operation(summary = "Get proposal by ID",
            description = "Retrieves a specific mark occurrence proposal by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proposal found.",
                    content = @Content(schema = @Schema(implementation = MarkOccurrenceProposalDto.class))),
            @ApiResponse(responseCode = "404", description = "Proposal not found.")
    })
    @GetMapping("/{proposalId}")
    public ResponseEntity<MarkOccurrenceProposalDto> findById(@PathVariable Long proposalId) {
        return proposalService.findById(proposalId)
                .map(markOccurrenceProposalMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create and submit a proposal",
            description = "Creates a new mark occurrence proposal and submits it immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proposal created and submitted successfully.",
                    content = @Content(schema = @Schema(implementation = MarkOccurrenceProposalDto.class)))
    })
    @PostMapping
    public ResponseEntity<MarkOccurrenceProposalDto> createAndSubmit(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestBody @Valid MarkOccurrenceProposalCreateDto dto
    ) {
        User user = User.builder().id(principal.getId()).build();
        MarkOccurrenceProposal proposal = submissionService.createAndSubmit(dto, user);
        return ResponseEntity.ok(markOccurrenceProposalMapper.toDto(proposal));
    }
}
