package pt.estga.user.controllers;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.user.dtos.UserPublicDto;
import pt.estga.user.services.UserService;

@RestController
@RequestMapping("/api/v1/public/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Public endpoints for users.")
public class UserPublicController {

    private final UserService service;

    @GetMapping("/{id}")
    public ResponseEntity<UserPublicDto> publicGetById(
            @Parameter(description = "ID of the user to be retrieved", required = true)
            @PathVariable UUID id) {
        return service.findPublicById(id)
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
