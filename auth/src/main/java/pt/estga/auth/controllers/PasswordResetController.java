package pt.estga.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.auth.dtos.PasswordResetRequestDto;
import pt.estga.shared.dtos.MessageResponseDto;
import pt.estga.verification.dtos.ResetPasswordRequestDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password-reset")
@Tag(name = "Password Reset", description = "Password reset moved to Keycloak.")
public class PasswordResetController {

    @Operation(summary = "Request password reset (REMOVED)",
               description = "Password reset is handled by Keycloak.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "410", description = "Gone - use Keycloak reset flow.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/request")
    public ResponseEntity<MessageResponseDto> requestPasswordReset(
            @RequestBody PasswordResetRequestDto request) {
        return ResponseEntity.status(410)
                .body(new MessageResponseDto("Password reset moved to Keycloak. Use Keycloak login page reset flow."));
    }

    @Operation(summary = "Reset password (REMOVED)",
               description = "Password reset is handled by Keycloak.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "410", description = "Gone - use Keycloak reset flow.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/reset")
    public ResponseEntity<MessageResponseDto> resetPassword(
            @RequestBody ResetPasswordRequestDto request) {
        return ResponseEntity.status(410)
                .body(new MessageResponseDto("Password reset moved to Keycloak. Use Keycloak login page reset flow."));
    }
}
