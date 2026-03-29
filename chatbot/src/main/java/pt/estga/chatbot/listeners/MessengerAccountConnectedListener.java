package pt.estga.chatbot.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.EmojiKey;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.services.notifications.MessengerNotificationService;
import pt.estga.chatbot.services.notifications.MessengerNotificationServiceFactory;
import pt.estga.verification.events.ChatbotAccountConnectedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessengerAccountConnectedListener {

    private final MessengerNotificationServiceFactory notificationServiceFactory;

    @EventListener
    @Async
    public void handleMessengerAccountConnected(ChatbotAccountConnectedEvent event) {
        log.info("Messenger account connected event received for user: {}, platform: {}, recipientId: {}",
                event.getUserId(), event.getPlatform(), event.getRecipientId());

        try {
            Platform platform = Platform.valueOf(event.getPlatform());
            MessengerNotificationService notificationService = notificationServiceFactory.getNotificationService(platform);
            notificationService.sendNotificationWithMenu(
                    event.getRecipientId(),
                    new Message(MessageKey.ACCOUNT_CONNECTED_NOTIFICATION, EmojiKey.TADA)
            );
        } catch (IllegalArgumentException e) {
            log.error("Unsupported platform in ChatbotAccountConnectedEvent: {}", event.getPlatform(), e);
        }
    }
}
