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
import pt.estga.shared.models.AppPrincipal;
import pt.estga.shared.utils.ServiceAccountUtils;

import java.util.Collections;
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

    private static final Long BOT_SERVICE_ACCOUNT_ID = 2L;

    @PostMapping
    public ResponseEntity<Void> onMessage(@RequestBody WhatsAppWebhookPayload payload) {
        try {
            log.info("Received WhatsApp payload: {}", payload);

            BotInput input = WhatsAppMapper.toBotMessage(payload);

            if (input == null) {
                log.info("Mapper returned null — no actionable message");
                return ResponseEntity.ok().build();
            }

            log.info("Mapped BotInput: {}", input);

            List<BotResponse> responses = botEngine.handleInput(input);
            if (responses == null || responses.isEmpty()) {
                log.info("BotEngine returned no responses");
                return ResponseEntity.ok().build();
            }

            // Use service account for sending messages back to WhatsApp
            AppPrincipal botPrincipal = AppPrincipal.builder()
                    .id(BOT_SERVICE_ACCOUNT_ID)
                    .identifier("WhatsAppBot")
                    .password(null)
                    .authorities(Collections.emptyList())
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();

            ServiceAccountUtils.runAsServiceAccount(botPrincipal, () -> {
                for (BotResponse response : responses) {
                    try {
                        log.info("Sending message to {}: {}", input.getChatId(), response.getTextNode());
                        apiClient.sendMessage(String.valueOf(input.getChatId()), response);
                    } catch (Exception e) {
                        log.error("Failed to send message to {}: {}", input.getChatId(), response, e);
                    }
                }
                return null;
            });

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

        log.info("WhatsApp webhook verification request received");
        log.info("Mode: {}, Challenge: {}, Token: {}", mode, challenge, token);
        log.info("Expected token: {}", verifyToken);

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verification successful");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("WhatsApp webhook verification failed");
            return ResponseEntity.badRequest().build();
        }
    }
}
