package pt.estga.chatbot.services.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.services.messages.UiTextService;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final StonemarkTelegramBot telegramBot;
    private final UiTextService uiTextService;

    public void sendNotification(String recipientId, Message message) {
        send(recipientId, uiTextService.get(message));
    }

    public void sendNotification(String recipientId, MessageKey messageKey, Object... args) {
        send(recipientId, uiTextService.get(messageKey, args));
    }

    public void sendNotificationWithMenu(String recipientId, Message message) {
        sendNotification(recipientId, message);
        sendMenu(recipientId);
    }

    public void sendNotificationWithMenu(String recipientId, MessageKey messageKey, Object... args) {
        sendNotification(recipientId, messageKey, args);
        sendMenu(recipientId);
    }

    private void send(String recipientId, RenderedText rendered) {
        try {
            SendMessage sendMessage = new SendMessage(recipientId, rendered.text());
            if (rendered.parseMode() != null) {
                sendMessage.setParseMode(rendered.parseMode());
            }
            telegramBot.execute(sendMessage);
            log.debug("Notification sent to Telegram user: {}", recipientId);
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to Telegram user {}: {}", recipientId, e.getMessage());
        }
    }

    private void sendMenu(String recipientId) {
        try {
            long chatId = Long.parseLong(recipientId);
            BotInput menuInput = BotInput.builder()
                    .userId(recipientId)
                    .chatId(chatId)
                    .platform(Platform.TELEGRAM)
                    .type(BotInput.InputType.CALLBACK)
                    .callbackData(CallbackData.BACK_TO_MAIN_MENU)
                    .build();
            telegramBot.dispatchAndSend(menuInput);
            log.debug("Main menu dispatched and sent to Telegram user: {}", recipientId);
        } catch (NumberFormatException e) {
            log.error("Invalid telegram recipient id '{}': {}", recipientId, e.getMessage());
        }
    }
}
