package pt.estga.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.estga.auth.dtos.*;
import pt.estga.auth.services.SocialAuthenticationService;
import pt.estga.shared.dtos.MessageResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Social authentication endpoints. Primary authentication via Keycloak.")
public class AuthenticationController {

    private final SocialAuthenticationService socialAuthenticationService;

    @Operation(summary = "Authenticate with Google (DEPRECATED - Migrate to Keycloak)",
               description = "⚠️ DEPRECATED: Social authentication should be configured in Keycloak as Identity Provider. " +
                             "This endpoint still works but will be removed in future versions.",
               deprecated = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful, returns JWT tokens.",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid Google token.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/google")
    @Deprecated
    public ResponseEntity<AuthenticationResponseDto> google(
            @RequestBody GoogleAuthenticationRequestDto request) {
        return socialAuthenticationService.authenticateWithGoogle(request.token())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @Operation(summary = "Authenticate with Telegram",
               description = "Authenticates a user using Telegram login data. If the user does not exist, a new account will be created.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful, returns JWT tokens.",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid Telegram data.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/telegram")
    public ResponseEntity<AuthenticationResponseDto> telegram(
            @RequestBody TelegramAuthenticationRequestDto request) {
        return socialAuthenticationService.authenticateWithTelegram(request.telegramData())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
