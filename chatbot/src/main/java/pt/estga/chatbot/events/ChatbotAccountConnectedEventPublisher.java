package pt.estga.chatbot.events;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pt.estga.verification.events.ChatbotAccountConnectedEvent;

@Component
@RequiredArgsConstructor
public class ChatbotAccountConnectedEventPublisher {
    private final ApplicationEventPublisher publisher;

    public void publish(String platform, String recipientId, Long userId) {
        publisher.publishEvent(new ChatbotAccountConnectedEvent(this, platform, recipientId, userId));
    }
}
