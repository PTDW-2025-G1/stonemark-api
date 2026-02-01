package pt.estga.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.user.dtos.UserContactDto;
import pt.estga.user.entities.User;
import pt.estga.user.entities.UserContact;
import pt.estga.user.mappers.UserContactMapper;
import pt.estga.user.services.UserContactService;
import pt.estga.user.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users/{userId}/contacts")
@RequiredArgsConstructor
@Tag(name = "User Contact Management", description = "Endpoints for managing user contacts by administrators.")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserContactController {

    private final UserService userService;
    private final UserContactService userContactService;
    private final UserContactMapper userContactMapper;

    @Operation(summary = "Get all contacts for a user", description = "Retrieves a list of all contacts for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of contacts"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping
    public ResponseEntity<List<UserContactDto>> getUserContacts(
            @Parameter(description = "The ID of the user", required = true)
            @PathVariable Long userId) {
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<UserContact> contacts = userContactService.findAllByUser(user);
        return ResponseEntity.ok(contacts.stream().map(userContactMapper::toDto).toList());
    }

    @Operation(summary = "Add a new contact to a user", description = "Adds a new contact to a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contact created successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping
    public ResponseEntity<UserContactDto> addUserContact(
            @Parameter(description = "The ID of the user", required = true)
            @PathVariable Long userId,
            @Parameter(description = "The contact to add", required = true)
            @RequestBody UserContactDto userContactDto) {
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        UserContact userContact = userContactMapper.toEntity(userContactDto);
        userContact.setUser(user);
        UserContact createdContact = userContactService.create(userContact);
        return ResponseEntity.status(201).body(userContactMapper.toDto(createdContact));
    }

    @Operation(summary = "Update a user's contact", description = "Updates an existing contact for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact updated successfully"),
            @ApiResponse(responseCode = "404", description = "User or contact not found")
    })
    @PutMapping("/{contactId}")
    public ResponseEntity<UserContactDto> updateUserContact(
            @Parameter(description = "The ID of the user", required = true)
            @PathVariable Long userId,
            @Parameter(description = "The ID of the contact to update", required = true)
            @PathVariable Long contactId,
            @Parameter(description = "The updated contact information", required = true)
            @RequestBody UserContactDto userContactDto) {
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        UserContact userContact = userContactService.findById(contactId).orElseThrow(() -> new RuntimeException("Contact not found"));

        if (!userContact.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Contact does not belong to the specified user.");
        }

        userContactMapper.updateEntity(userContactDto, userContact);
        UserContact updatedContact = userContactService.update(userContact);
        return ResponseEntity.ok(userContactMapper.toDto(updatedContact));
    }

    @Operation(summary = "Delete a user's contact", description = "Deletes a contact for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Contact deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User or contact not found")
    })
    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteUserContact(
            @Parameter(description = "The ID of the user", required = true)
            @PathVariable Long userId,
            @Parameter(description = "The ID of the contact to delete", required = true)
            @PathVariable Long contactId) {
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        UserContact userContact = userContactService.findById(contactId).orElseThrow(() -> new RuntimeException("Contact not found"));

        if (!userContact.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Contact does not belong to the specified user.");
        }

        userContactService.deleteById(contactId);
        return ResponseEntity.noContent().build();
    }
}
