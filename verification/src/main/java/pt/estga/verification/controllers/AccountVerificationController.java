package pt.estga.verification.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.shared.interfaces.AuthenticatedPrincipal;
import pt.estga.user.entities.User;
import pt.estga.user.services.ChatbotAccountService;
import pt.estga.user.services.UserService;
import pt.estga.verification.dtos.ChatbotVerificationRequestDto;
import pt.estga.verification.dtos.ChatbotVerificationResponseDto;
import pt.estga.verification.events.ChatbotAccountConnectedEvent;
import pt.estga.verification.services.ChatbotVerificationService;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Account-related endpoints (verification, profile, etc.)")
public class AccountVerificationController {

    private final ChatbotVerificationService verificationService;
    private final UserService userService;
    private final ChatbotAccountService chatbotAccountService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/verification/chatbot")
    @Operation(summary = "Verify Chatbot code", description = "Verifies code from chatbot and links the messaging account to current authenticated user")
    public ResponseEntity<ChatbotVerificationResponseDto> verifyChatbotCode(
            @RequestBody ChatbotVerificationRequestDto request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {

        Optional<String> platformUserIdOpt = verificationService.verifyAndGetPlatformUserId(request.code());

        if (platformUserIdOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ChatbotVerificationResponseDto.error("Invalid or expired code")
            );
        }

        String platformUserId = platformUserIdOpt.get();
        User user = userService.findById(principal.getId()).orElseThrow();

        // Create or update chatbot identity (platform-agnostic)
        chatbotAccountService.createOrUpdateChatbot(user, platformUserId);

        // Publish event for chatbot to handle notification, platform set to TELEGRAM for now
        eventPublisher.publishEvent(new ChatbotAccountConnectedEvent(this, "TELEGRAM", platformUserId, user.getId()));

        return ResponseEntity.ok(
                ChatbotVerificationResponseDto.success("Messaging account linked successfully")
        );
    }
}
