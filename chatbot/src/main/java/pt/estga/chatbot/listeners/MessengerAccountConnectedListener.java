package pt.estga.chatbot.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.services.notifications.MessengerNotificationService;
import pt.estga.verification.events.ChatbotAccountConnectedEvent;

import java.util.List;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessengerAccountConnectedListener {

    private final List<MessengerNotificationService> notificationServices;

    @ApplicationModuleListener
    public void handleMessengerAccountConnected(ChatbotAccountConnectedEvent event) {
        log.info("Messenger account connected event received for user: {}, platform: {}, recipientId: {}",
                event.getUserId(), event.getPlatform(), event.getRecipientId());

        try {
            Platform platform = Platform.valueOf(event.getPlatform());
            MessengerNotificationService notificationService = resolveNotificationService(platform);
            notificationService.sendNotificationWithMenu(
                    event.getRecipientId(),
                    MessageKey.ACCOUNT_CONNECTED_NOTIFICATION
            );
        } catch (IllegalArgumentException e) {
            log.error("Unsupported platform in ChatbotAccountConnectedEvent: {}", event.getPlatform(), e);
        }
    }

    private MessengerNotificationService resolveNotificationService(Platform platform) {
        return notificationServices.stream()
                .filter(s -> s.supports(platform))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No MessengerNotificationService found for platform: " + platform));
    }
}
