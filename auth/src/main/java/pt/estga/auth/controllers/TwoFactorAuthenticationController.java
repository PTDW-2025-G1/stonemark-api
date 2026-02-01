package pt.estga.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.auth.dtos.SetTfaMethodRequestDto;
import pt.estga.auth.dtos.TfaSetupResponseDto;
import pt.estga.auth.dtos.TfaVerificationRequestDto;
import pt.estga.auth.services.tfa.TotpService;
import pt.estga.auth.services.tfa.TwoFactorAuthenticationService;
import pt.estga.shared.dtos.MessageResponseDto;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.entities.User;
import pt.estga.user.enums.TfaMethod;
import pt.estga.user.services.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/tfa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Authentication", description = "Operations related to Two-Factor Authentication (2FA) for user accounts.")
@PreAuthorize("isAuthenticated()")
public class TwoFactorAuthenticationController {

    private final TotpService totpService;
    private final TwoFactorAuthenticationService twoFactorAuthenticationService;
    private final UserService userService;

    @Operation(summary = "Setup TOTP 2FA",
               description = "Initiates the setup process for Time-based One-Time Password (TOTP) 2FA, returning a QR code URI and secret.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TOTP setup initiated successfully.",
                    content = @Content(schema = @Schema(implementation = TfaSetupResponseDto.class)))
    })
    @PostMapping("/setup/totp")
    public ResponseEntity<TfaSetupResponseDto> setupTotp(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User user = userService.findById(principal.getId()).orElseThrow();
        TfaSetupResponseDto response = totpService.setupTotpForUser(user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Enable TOTP 2FA",
               description = "Enables TOTP 2FA for the authenticated user after verifying a provided TOTP code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Two-Factor Authentication enabled successfully.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid 2FA code or TOTP not set up.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/enable/totp")
    public ResponseEntity<?> enableTotp(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody TfaVerificationRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        if (user.getTfaSecret() == null || !totpService.isCodeValid(user.getTfaSecret(), request.code())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponseDto("Invalid 2FA code."));
        }
        totpService.enableTfa(user, TfaMethod.TOTP);
        return ResponseEntity.ok(new MessageResponseDto("Two-Factor Authentication enabled successfully."));
    }

    @Operation(summary = "Disable 2FA",
               description = "Disables the currently active 2FA method for the authenticated user after verifying a provided code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Two-Factor Authentication disabled successfully.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - 2FA not enabled or invalid 2FA code.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/disable")
    public ResponseEntity<?> disableTfa(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody TfaVerificationRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        if (user.getTfaMethod() == TfaMethod.NONE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponseDto("Two-Factor Authentication is not enabled."));
        }

        if (!twoFactorAuthenticationService.verifyAndDisableCurrentTfa(user, request.code())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponseDto("Invalid 2FA code."));
        }

        return ResponseEntity.ok(new MessageResponseDto("Two-Factor Authentication disabled successfully."));
    }

    @Operation(summary = "Set 2FA method",
               description = "Sets the preferred Two-Factor Authentication method (e.g., TOTP, SMS) for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Two-Factor Authentication method updated successfully.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - TOTP secret not set if trying to enable TOTP.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/method")
    public ResponseEntity<?> setTfaMethod(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody SetTfaMethodRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        if (request.tfaMethod() == TfaMethod.TOTP && user.getTfaSecret() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponseDto("TOTP secret not set. Please set up TOTP first."));
        }
        twoFactorAuthenticationService.setTfaMethod(user, request.tfaMethod());
        return ResponseEntity.ok(new MessageResponseDto("Two-Factor Authentication method updated successfully."));
    }

    @Operation(summary = "Request 2FA code via contact",
               description = "Requests a Two-Factor Authentication code to be sent to the user's primary contact method (e.g., email, phone).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "2FA code sent to your primary contact method.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - No primary contact method found or other illegal state.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/contact/request-code")
    public ResponseEntity<?> requestContactTfaCode(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User user = userService.findById(principal.getId()).orElseThrow();
        try {
            twoFactorAuthenticationService.requestTfaContactCode(user);
            return ResponseEntity.ok(new MessageResponseDto("2FA code sent to your primary contact method."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponseDto(e.getMessage()));
        }
    }

    @Operation(summary = "Get 2FA status")
    @GetMapping("/status")
    public ResponseEntity<?> getTfaStatus(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User user = userService.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "enabled", user.getTfaMethod() != TfaMethod.NONE,
                "method", user.getTfaMethod()
        ));
    }


    @Operation(summary = "Verify 2FA code from contact",
               description = "Verifies the Two-Factor Authentication code received via the user's primary contact method.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "2FA code verified successfully.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid 2FA code or other illegal state.",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })
    @PostMapping("/contact/verify-code")
    public ResponseEntity<?> verifyContactTfaCode(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody TfaVerificationRequestDto request) {
        User user = userService.findById(principal.getId()).orElseThrow();
        try {
            if (twoFactorAuthenticationService.verifyTfaContactCode(user, request.code())) {
                return ResponseEntity.ok(new MessageResponseDto("2FA code verified successfully."));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponseDto("Invalid 2FA code."));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponseDto(e.getMessage()));
        }
    }
}
