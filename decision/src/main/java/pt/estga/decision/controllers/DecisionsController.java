package pt.estga.decision.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.decision.dtos.ActiveDecisionViewDto;
import pt.estga.decision.dtos.ManualDecisionRequest;
import pt.estga.decision.mappers.DecisionMapper;
import pt.estga.decision.repositories.ProposalDecisionAttemptRepository;
import pt.estga.decision.services.DecisionServiceFactory;
import pt.estga.decision.services.ProposalDecisionService;
import pt.estga.shared.exceptions.InvalidCredentialsException;
import pt.estga.shared.exceptions.ResourceNotFoundException;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/proposals/{id}/decisions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REVIEWER')")
@Tag(name = "Proposal Decisions", description = "Endpoints for managing proposal decisions.")
public class DecisionsController {

    private final DecisionServiceFactory decisionServiceFactory;
    private final ProposalDecisionAttemptRepository attemptRepo;
    private final UserService userService;
    private final DecisionMapper decisionMapper;

    @Operation(summary = "Get active decision for proposal",
               description = "Retrieves the latest decision attempt for a proposal.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active decision retrieved successfully.",
                    content = @Content(schema = @Schema(implementation = ActiveDecisionViewDto.class))),
            @ApiResponse(responseCode = "404", description = "No active decision found.")
    })
    @GetMapping("/active")
    public ResponseEntity<ActiveDecisionViewDto> getActiveDecision(@PathVariable Long id) {
        return attemptRepo.findFirstByProposalIdOrderByDecidedAtDesc(id)
                .map(decisionMapper::toActiveDecisionViewDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get decision history for proposal",
               description = "Retrieves the history of decision attempts for a proposal.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decision history retrieved successfully.",
                    content = @Content(schema = @Schema(implementation = ActiveDecisionViewDto.class)))
    })
    @GetMapping("/history")
    public ResponseEntity<List<ActiveDecisionViewDto>> getDecisionHistory(@PathVariable Long id) {
        return ResponseEntity.ok(attemptRepo.findByProposalIdOrderByDecidedAtDesc(id)
                .stream()
                .map(decisionMapper::toActiveDecisionViewDto)
                .toList());
    }

    @Operation(summary = "Create a manual decision",
               description = "Creates a manual decision for a proposal (Accept/Reject).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manual decision created successfully."),
            @ApiResponse(responseCode = "404", description = "Proposal not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("/manual")
    public ResponseEntity<Void> createManualDecision(
            @PathVariable Long id,
            @RequestBody @Valid ManualDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal
    ) {
        if (principal == null) {
            throw new InvalidCredentialsException("User not authenticated");
        }
        var moderator = userService.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Moderator not found"));
        
        ProposalDecisionService<?> decisionService = decisionServiceFactory.getServiceForProposalId(id);
        decisionService.makeManualDecision(id, request.outcome(), request.notes(), moderator);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rerun automatic decision",
               description = "Triggers the automatic decision logic again for a proposal.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Automatic decision rerun successfully."),
            @ApiResponse(responseCode = "404", description = "Proposal not found.")
    })
    @PostMapping("/automatic/rerun")
    public ResponseEntity<Void> rerunAutomaticDecision(@PathVariable Long id) {
        ProposalDecisionService<?> decisionService = decisionServiceFactory.getServiceForProposalId(id);
        decisionService.makeAutomaticDecision(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Activate a previous decision",
               description = "Reverts the proposal status to a previous decision attempt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decision activated successfully."),
            @ApiResponse(responseCode = "404", description = "Proposal or decision attempt not found."),
            @ApiResponse(responseCode = "400", description = "Decision attempt does not belong to the proposal.")
    })
    @PostMapping("/{attemptId}/activate")
    public ResponseEntity<Void> activateDecision(
            @PathVariable Long id,
            @PathVariable Long attemptId
    ) {
        var attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision attempt not found with id: " + attemptId));
        
        if (!attempt.getProposal().getId().equals(id)) {
            // Returning 400 Bad Request for mismatched proposal ID
            return ResponseEntity.badRequest().build();
        }
        
        ProposalDecisionService<?> decisionService = decisionServiceFactory.getServiceForProposalId(id);
        decisionService.activateDecision(attemptId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deactivate current decision",
               description = "Reverts the proposal status to UNDER_REVIEW, effectively removing the current decision.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decision deactivated successfully."),
            @ApiResponse(responseCode = "404", description = "Proposal not found.")
    })
    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateDecision(@PathVariable Long id) {
        ProposalDecisionService<?> decisionService = decisionServiceFactory.getServiceForProposalId(id);
        decisionService.deactivateDecision(id);
        return ResponseEntity.ok().build();
    }
}
