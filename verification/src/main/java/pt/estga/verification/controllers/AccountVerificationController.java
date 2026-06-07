package pt.estga.verification.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pt.estga.sharedcore.events.AfterCommitEventPublisher;
import pt.estga.sharedcore.interfaces.AuthenticatedPrincipal;
import pt.estga.commonweb.dtos.MessageResponseDto;
import pt.estga.userapi.ChatbotAccountOperations;
import pt.estga.verification.dtos.ChatbotVerificationRequestDto;
import pt.estga.verification.events.ChatbotAccountConnectedEvent;
import pt.estga.verification.services.ChatbotVerificationService;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Account-related endpoints (verification, profile, etc.)")
public class AccountVerificationController {

    private final ChatbotVerificationService verificationService;
    private final ChatbotAccountOperations chatbotAccountOps;
    private final AfterCommitEventPublisher eventPublisher;

    @PostMapping("/verification/chatbot")
    @Operation(summary = "Verify Chatbot code", description = "Verifies code from chatbot and links the messaging account to current authenticated user")
    public ResponseEntity<MessageResponseDto> verifyChatbotCode(
            @Valid @RequestBody ChatbotVerificationRequestDto request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {

        Optional<String> platformUserIdOpt = verificationService.verifyAndGetPlatformUserId(request.code());

        if (platformUserIdOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    MessageResponseDto.error("Invalid or expired code")
            );
        }

        String platformUserId = platformUserIdOpt.get();
        Long userId = principal.getId();

        chatbotAccountOps.createOrUpdateChatbot(userId, platformUserId);

        eventPublisher.publish(new ChatbotAccountConnectedEvent(this, "TELEGRAM", platformUserId, userId));

        return ResponseEntity.ok(
                MessageResponseDto.success("Messaging account linked successfully")
        );
    }
}
