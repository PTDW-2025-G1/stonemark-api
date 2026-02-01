package pt.estga.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import pt.estga.shared.aop.SensitiveOperation;
import pt.estga.shared.dtos.MessageResponseDto;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.dtos.ContactDto;
import pt.estga.user.dtos.UserContactDto;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserContactMapper;
import pt.estga.user.services.AccountService;
import pt.estga.user.services.UserContactService;
import pt.estga.user.services.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/account/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Endpoints for managing user contacts.")
@PreAuthorize("isAuthenticated()")
public class AccountContactController {

    private final UserService userService;
    private final AccountService accountService;
    private final UserContactService userContactService;
    private final UserContactMapper userContactMapper;

    @Operation(summary = "Add a new contact", description = "Adds a new contact to the authenticated user's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact added successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid contact details provided")
    })
    @PostMapping
    public ResponseEntity<?> addContact(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Parameter(description = "Contact details to be added", required = true)
            @Valid @RequestBody ContactDto request) {
        User user = userService.findById(principal.getId())
                .orElseThrow();
        accountService.addContact(user, request.value(), request.type());
        return ResponseEntity.ok(new MessageResponseDto("A new contact has been added to your account."));
    }

    @Operation(summary = "Get all contacts", description = "Retrieves a list of all contacts for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of contacts",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserContactDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserContactDto>> getContacts(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User user = userService.findByIdWithContacts(principal.getId())
                .orElseThrow();
        List<UserContactDto> contactDtos = accountService.getContacts(user).stream()
                .map(userContactMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(contactDtos);
    }

    @Operation(summary = "Check if a contact exists", description = "Checks if a contact exists by its value.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns true if the contact exists, false otherwise")
    })
    @GetMapping("/exists/by-value")
    public ResponseEntity<Boolean> existsByValue(
            @Parameter(description = "The value of the contact to check", required = true)
            @RequestParam String value) {
        return ResponseEntity.ok(userContactService.existsByValue(value));
    }

    @Operation(summary = "Request contact verification", description = "Sends a verification message to the specified contact.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification message sent successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @PostMapping("/{id}/verify")
    public ResponseEntity<?> requestContactVerification(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Parameter(description = "The ID of the contact to verify", required = true)
            @PathVariable Long id) {
        User user = userService.findById(principal.getId())
                .orElseThrow();
        accountService.requestContactVerification(user, id);
        return ResponseEntity.ok(new MessageResponseDto("A verification message has been sent."));
    }

    @Operation(
            summary = "Set primary contact",
            description = "Sets a verified contact as the primary contact for its type (EMAIL or TELEPHONE)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact set as primary",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Contact not verified or invalid"),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @PatchMapping("/{contactId}/primary")
    public ResponseEntity<?> setPrimaryContact(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long contactId
    ) {
        User user = userService.findById(principal.getId())
                .orElseThrow();
        accountService.setPrimaryContact(user, contactId);
        return ResponseEntity.ok(new MessageResponseDto("Contact set as primary."));
    }

    @SensitiveOperation
    @Operation(summary = "Delete a contact", description = "Deletes a contact from the authenticated user's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact deleted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @DeleteMapping("/{contactId}")
    public ResponseEntity<?> deleteContact(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Parameter(description = "The ID of the contact to be deleted", required = true)
            @PathVariable Long contactId) {
        User user = userService.findById(principal.getId())
                .orElseThrow();
        accountService.deleteContact(user, contactId);
        return ResponseEntity.ok(new MessageResponseDto("Contact has been deleted from your account."));
    }
}
