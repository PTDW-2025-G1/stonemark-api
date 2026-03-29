package pt.estga.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.mappers.UserMapper;
import pt.estga.user.services.UserQueryService;

@RestController
@RequestMapping("/api/v1/public/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Public endpoints for users.")
public class PublicUserController {

    private final UserQueryService service;
    private final UserMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<UserPublicDto> publicGetById(
            @Parameter(description = "ID of the user to be retrieved", required = true)
            @PathVariable Long id) {
        return service.findById(id)
                .map(mapper::toPublicDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Check if a user exists by username", description = "Checks if a user exists with the given username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns true if the user exists, false otherwise")
    })
    @GetMapping("/exists/by-username")
    public ResponseEntity<Boolean> existsByUsername(
            @Parameter(description = "Username to check for existence", required = true)
            @RequestParam String username) {
        return ResponseEntity.ok(service.existsByUsername(username));
    }
}
