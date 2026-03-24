package pt.estga.chatbot.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;

@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramTestController {

    private final ObjectMapper objectMapper;
    private final StonemarkTelegramBot telegramBot;

    @PostMapping("/test")
    public ResponseEntity<String> testWebhook(HttpServletRequest request) {
        try {
            String body = request.getReader().lines().reduce((a, b) -> a + b).orElse("");
            log.info("/telegram/test received body: {}", body);
            Update update = objectMapper.readValue(body, Update.class);
            log.info("Parsed Update in test endpoint: updateId={}", update == null ? "<null>" : update.getUpdateId());
            BotApiMethod<?> result = telegramBot.onWebhookUpdateReceived(update);
            log.info("telegramBot.onWebhookUpdateReceived returned: {}", result);
            return ResponseEntity.ok("dispatched");
        } catch (IOException e) {
            log.error("Error reading test request body", e);
            return ResponseEntity.badRequest().body("invalid");
        } catch (Exception e) {
            log.error("Error handling test webhook", e);
            return ResponseEntity.status(500).body("error");
        }
    }
}
