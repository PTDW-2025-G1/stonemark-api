package pt.estga;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.estga.notification.models.Email;
import pt.estga.notification.services.EmailService;
import pt.estga.commonweb.dtos.MessageResponseDto;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/email")
@RequiredArgsConstructor
@Tag(name = "Email Admin", description = "Admin endpoints for email system.")
@PreAuthorize("isAuthenticated()")
public class EmailAdminController {

    private final EmailService emailService;

    @PostMapping("/test")
    public ResponseEntity<MessageResponseDto> sendTestEmail(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(MessageResponseDto.error("Recipient email is required"));
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
