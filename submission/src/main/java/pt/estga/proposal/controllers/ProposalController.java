package pt.estga.proposal.controllers;

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
import pt.estga.proposal.dtos.ProposalSummaryDto;
import pt.estga.proposal.mappers.ProposalMapper;
import pt.estga.proposal.projections.ProposalStatsProjection;
import pt.estga.proposal.services.ProposalService;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.entities.User;

@RestController
@RequestMapping("/api/v1/public/proposals")
@RequiredArgsConstructor
@Tag(name = "Proposals (Generic)", description = "Generic endpoints for listing and querying all types of proposals.")
public class ProposalController {

    private final ProposalService proposalService;
    private final ProposalMapper proposalMapper;

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
        return proposalService.findByUser(user, PageRequest.of(page, size))
                .map(proposalMapper::toSummaryDto);
    }

    @Operation(summary = "Get user proposal statistics",
            description = "Retrieves global statistics about proposals for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully.",
                    content = @Content(schema = @Schema(implementation = ProposalStatsProjection.class)))
    })
    @GetMapping("/user/me/stats")
    public ResponseEntity<ProposalStatsProjection> getUserStats(
            @AuthenticationPrincipal AuthenticatedPrincipal principal
    ) {
        User user = User.builder().id(principal.getId()).build();
        return ResponseEntity.ok(proposalService.getStatsByUser(user));
    }

    @Operation(summary = "Get proposal summary by ID",
            description = "Retrieves a basic summary of a proposal by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proposal found.",
                    content = @Content(schema = @Schema(implementation = ProposalSummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "Proposal not found.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProposalSummaryDto> findById(@PathVariable Long id) {
        return proposalService.findById(id)
                .map(proposalMapper::toSummaryDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
