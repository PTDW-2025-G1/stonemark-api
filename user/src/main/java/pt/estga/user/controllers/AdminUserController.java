package pt.estga.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.filterutils.models.PagedRequest;
import pt.estga.user.dtos.UserDto;
import pt.estga.user.entities.User;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.services.UserQueryService;
import pt.estga.user.services.UserService;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users (Admin).")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService service;
    private final UserQueryService queryService;
    private final UserMapper mapper;

    @Operation(summary = "Search users", description = "Searches for users based on dynamic filters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)))
    })
    @PostMapping("/search")
    public ResponseEntity<Page<UserDto>> search(@RequestBody PagedRequest request) {
        return ResponseEntity.ok(queryService.search(request));
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(
            @Parameter(description = "ID of the user to be retrieved", required = true)
            @PathVariable Long id) {
        return service.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a user", description = "Updates the details of an existing user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> update(
            @Parameter(description = "ID of the user to be updated", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated user details", required = true)
            @RequestBody UserDto userDto) {
        User user = mapper.toEntity(userDto);
        user.setId(id);
        return ResponseEntity.ok(mapper.toDto(service.update(user)));
    }

    @Operation(summary = "Delete a user", description = "Deletes a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(
            @Parameter(description = "ID of the user to be deleted", required = true)
            @PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
