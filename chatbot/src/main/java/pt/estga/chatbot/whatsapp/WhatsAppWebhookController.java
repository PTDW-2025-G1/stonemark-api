package pt.estga.chatbot.whatsapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.services.BotEngine;

import java.util.List;

@RestController
@RequestMapping("${whatsapp.bot.webhook-path}")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final BotEngine botEngine;
    private final WhatsAppApiClient apiClient;

    @Value("${whatsapp.bot.token}")
    private String verifyToken;

    @PostMapping
    public ResponseEntity<Void> onMessage(@RequestBody WhatsAppWebhookPayload payload) {
        try {
            BotInput input = WhatsAppMapper.toBotMessage(payload);

            if (input == null) {
                return ResponseEntity.ok().build();
            }

            List<BotResponse> responses = botEngine.handleInput(input);
            if (responses == null || responses.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            for (BotResponse response : responses) {
                try {
                    apiClient.sendMessage(String.valueOf(input.getChatId()), response);
                } catch (Exception e) {
                    log.error("Failed to send message to {}: {}", input.getChatId(), response, e);
                }
            }

        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook", e);
        } finally {
            SecurityContextHolder.clearContext();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("WhatsApp webhook verification failed");
            return ResponseEntity.badRequest().build();
        }
    }
}
