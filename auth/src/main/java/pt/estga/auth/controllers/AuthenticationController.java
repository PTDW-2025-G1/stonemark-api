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
import pt.estga.auth.dtos.*;
import pt.estga.shared.dtos.MessageResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "All authentication via Keycloak. Social auth endpoints removed.")
public class AuthenticationController {

    @Operation(summary = "Authenticate with Telegram (NOT IMPLEMENTED)",
               description = "Telegram authentication was never fully implemented. Use Keycloak for authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "410", description = "Gone - Not implemented.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/telegram")
    public ResponseEntity<MessageResponseDto> telegram(
            @RequestBody TelegramAuthenticationRequestDto request) {
        return ResponseEntity.status(410)
                .body(new MessageResponseDto(
                    "Telegram authentication was never implemented. Use Keycloak for all authentication."
                ));
    }
}
