package pt.estga.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.auth.dto.AuthResponse;
import pt.estga.auth.dto.LoginRequest;
import pt.estga.auth.dto.RefreshTokenRequest;
import pt.estga.auth.service.AuthService;
import pt.estga.shared.core.interfaces.AuthenticatedPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate with username and password")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        if (principal != null && principal.getId() != null) {
            authService.logout(principal.getId());
        }
        return ResponseEntity.ok().build();
    }
}
