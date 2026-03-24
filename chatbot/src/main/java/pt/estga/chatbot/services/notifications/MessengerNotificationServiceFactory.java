package pt.estga.chatbot.services.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.models.Platform;

import java.util.List;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class MessengerNotificationServiceFactory {

    private final List<MessengerNotificationService> notificationServices;

    public MessengerNotificationService getNotificationService(Platform platform) {
        return notificationServices.stream()
                .filter(service -> service.supports(platform))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No MessengerNotificationService found for platform: " + platform));
    }
}
