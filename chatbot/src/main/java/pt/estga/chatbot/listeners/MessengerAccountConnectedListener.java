package pt.estga.chatbot.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.EmojiKey;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.services.TelegramNotificationService;
import pt.estga.verification.events.MessengerAccountConnectedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessengerAccountConnectedListener {

    private final TelegramNotificationService telegramNotificationService;

    @EventListener
    @Async
    public void handleMessengerAccountConnected(MessengerAccountConnectedEvent event) {
        log.info("Messenger account connected event received for user: {}, telegramId: {}",
                event.getUserId(), event.getTelegramId());

        // Send success notification to the user's Telegram
        telegramNotificationService.sendNotification(
                event.getTelegramId(),
                new Message(MessageKey.ACCOUNT_CONNECTED_NOTIFICATION, EmojiKey.TADA)
        );
    }
}


