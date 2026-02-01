package pt.estga.verification.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;
import pt.estga.verification.dtos.ConfirmationResponseDto;
import pt.estga.verification.dtos.VerificationRequestDto;
import pt.estga.verification.entities.ActionCode;
import pt.estga.verification.services.ChatbotVerificationService;
import pt.estga.verification.services.VerificationProcessingService;
import pt.estga.shared.exceptions.VerificationErrorMessages;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/account-verification")
@Tag(name = "Account Verification", description = "Endpoints for user account verification.")
public class AccountVerificationController {

    private final VerificationProcessingService verificationProcessingService;
    private final ChatbotVerificationService verificationService;
    private final UserService userService;

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmationResponseDto> confirm(@RequestBody VerificationRequestDto request) {
        Optional<String> resultToken = verificationProcessingService.confirmCode(request.code());
        return resultToken.map(t -> ResponseEntity.ok(ConfirmationResponseDto.passwordResetRequired(t)))
                .orElseGet(() -> ResponseEntity.ok(ConfirmationResponseDto.success(VerificationErrorMessages.CONFIRMATION_SUCCESSFUL)));
    }

    @PostMapping("/telegram/generate")
    @Operation(summary = "Generate Telegram verification code", description = "Generates a code for the authenticated user to verify their Telegram account.")
    public ResponseEntity<Map<String, String>> generateTelegramCode(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        User user = userService.findById(principal.getId()).orElseThrow();
        ActionCode actionCode = verificationService.generateTelegramVerificationCode(user);
        return ResponseEntity.ok(Map.of("code", actionCode.getCode()));
    }
}
