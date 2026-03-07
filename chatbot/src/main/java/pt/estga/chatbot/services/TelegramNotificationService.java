package pt.estga.chatbot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;
import pt.estga.chatbot.telegram.services.TelegramTextService;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.text.RenderedText;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final StonemarkTelegramBot telegramBot;
    private final UiTextService uiTextService;
    private final TelegramTextService telegramTextService;

    /**
     * Send a notification message to a Telegram user
     * @param telegramId The Telegram user ID
     * @param message The message to send
     */
    public void sendNotification(String telegramId, Message message) {
        try {
            RenderedText rendered = telegramTextService.render(uiTextService.get(message));

            SendMessage sendMessage = new SendMessage(telegramId, rendered.text());
            if (rendered.parseMode() != null) {
                sendMessage.setParseMode(rendered.parseMode());
            }

            telegramBot.execute(sendMessage);
            log.info("Notification sent to Telegram user: {}", telegramId);
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to Telegram user {}: {}", telegramId, e.getMessage());
        }
    }
}

