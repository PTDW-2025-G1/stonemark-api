package pt.estga.chatbot.services.notifications;

import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.Platform;

public interface MessengerNotificationService {

    boolean supports(Platform platform);

    void sendNotification(String recipientId, Message message);

    void sendNotificationWithMenu(String recipientId, Message message);
}
