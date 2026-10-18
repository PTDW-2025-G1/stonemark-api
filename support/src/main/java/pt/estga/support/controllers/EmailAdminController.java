package pt.estga.support.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.shared.web.dtos.ApiErrorResponse;
import pt.estga.shared.web.dtos.MessageResponseDto;
import pt.estga.support.notification.models.Email;
import pt.estga.support.notification.services.EmailService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/email")
@RequiredArgsConstructor
@Tag(name = "Email Admin", description = "Admin endpoints for email system.")
@PreAuthorize("isAuthenticated()")
public class EmailAdminController {

    private final EmailService emailService;

    @PostMapping("/test")
    public ResponseEntity<Object> sendTestEmail(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.of(400, "BAD_REQUEST", "Recipient email is required"));
        }

        var email = Email.builder()
                .to(to)
                .subject("Stonemark — Test Email")
                .template("test-email")
                .properties(Map.of())
                .build();

        emailService.sendEmail(email);
        return ResponseEntity.ok(MessageResponseDto.success("Test email sent to " + to));
    }
}
