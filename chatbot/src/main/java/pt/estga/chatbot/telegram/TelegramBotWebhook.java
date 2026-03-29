package pt.estga.chatbot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TelegramBotWebhook {

    private final StonemarkTelegramBot telegramBot;

    @PostMapping("${telegram.bot.webhook-path}")
    public ResponseEntity<Void> handleUpdate(HttpServletRequest request, @RequestBody(required = false) Update update) {
        try {
            if (update == null) {
                log.info("Received null Telegram update");
            } else if (update.hasCallbackQuery() && update.getCallbackQuery() != null) {
                log.info("Received Telegram callback query id={} data={}", update.getCallbackQuery().getId(), update.getCallbackQuery().getData());
            } else if (update.hasMessage() && update.getMessage() != null) {
                log.info("Received Telegram message updateId={} chatId={}", update.getUpdateId(), update.getMessage().getChatId());
            } else {
                log.info("Received Telegram update: updateId={}", update.getUpdateId());
            }

            log.debug("Full Update payload: {}", update);

            // If the filter pre-dispatched the Update, avoid double-processing but still return OK
            Object dispatched = request.getAttribute("BOT_DISPATCHED");
            if (Boolean.TRUE.equals(dispatched)) {
                log.info("Skipping controller dispatch because filter already dispatched the Update");
                return ResponseEntity.ok().build();
            }

            // Call bot processing asynchronously/synchronously as implemented in StonemarkTelegramBot
            try {
                telegramBot.onWebhookUpdateReceived(update);
                // Mark request as dispatched so the filter fallback does not double-dispatch.
                request.setAttribute("BOT_DISPATCHED", Boolean.TRUE);
            } catch (Exception e) {
                // Log but do not fail the HTTP response, as processing is asynchronous.
                log.error("Error invoking bot processing", e);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing Telegram update in controller", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            // Clear security context set by previous filters/threads to avoid leaking authentication into other processing.
            SecurityContextHolder.clearContext();
        }
    }
}
