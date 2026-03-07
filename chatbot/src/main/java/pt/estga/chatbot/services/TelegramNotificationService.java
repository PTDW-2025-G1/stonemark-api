package pt.estga.chatbot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.constants.SharedCallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.models.text.RenderedText;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;
import pt.estga.chatbot.telegram.services.TelegramTextService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService implements MessengerNotificationService {

    private final StonemarkTelegramBot telegramBot;
    private final UiTextService uiTextService;
    private final TelegramTextService telegramTextService;

    @Override
    public boolean supports(Platform platform) {
        return Platform.TELEGRAM.equals(platform);
    }

    @Override
    public void sendNotification(String recipientId, Message message) {
        try {
            RenderedText rendered = telegramTextService.render(uiTextService.get(message));

            SendMessage sendMessage = new SendMessage(recipientId, rendered.text());
            if (rendered.parseMode() != null) {
                sendMessage.setParseMode(rendered.parseMode());
            }

            telegramBot.execute(sendMessage);
            log.info("Notification sent to Telegram user: {}", recipientId);
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to Telegram user {}: {}", recipientId, e.getMessage());
        }
    }

    @Override
    public void sendNotificationWithMenu(String recipientId, Message message) {
        sendNotification(recipientId, message);

        try {
            long chatId = Long.parseLong(recipientId);

            // Ask dispatcher for the canonical menu flow instead of rendering menu here.
            BotInput menuInput = BotInput.builder()
                    .userId(recipientId)
                    .chatId(chatId)
                    .platform(Platform.TELEGRAM)
                    .type(BotInput.InputType.CALLBACK)
                    .callbackData(SharedCallbackData.BACK_TO_MAIN_MENU)
                    .build();

            telegramBot.dispatchAndSend(menuInput);
            log.info("Main menu dispatched and sent to Telegram user: {}", recipientId);
        } catch (NumberFormatException e) {
            log.error("Invalid telegram recipient id '{}': {}", recipientId, e.getMessage());
        }
    }
}
