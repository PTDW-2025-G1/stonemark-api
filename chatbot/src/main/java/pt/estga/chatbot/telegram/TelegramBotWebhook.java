package pt.estga.chatbot.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TelegramBotWebhook {

    private final StonemarkTelegramBot telegramBot;
    private final ObjectMapper objectMapper;

    @PostMapping("${telegram.bot.webhook-path}")
    public ResponseEntity<Void> handleUpdate(@RequestBody String body) {
        try {
            Update update = objectMapper.readValue(body, Update.class);

            if (update.hasCallbackQuery() && update.getCallbackQuery() != null) {
                log.debug("Received Telegram callback query id={}", update.getCallbackQuery().getId());
            } else if (update.hasMessage() && update.getMessage() != null) {
                log.debug("Received Telegram message updateId={} chatId={}", update.getUpdateId(), update.getMessage().getChatId());
            } else {
                log.debug("Received Telegram update: updateId={}", update.getUpdateId());
            }

            log.trace("Full Update payload: {}", update);

            telegramBot.onWebhookUpdateReceived(update);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing Telegram update in controller", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
