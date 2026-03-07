package pt.estga.verification.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MessengerAccountConnectedEvent extends ApplicationEvent {

    private final String platform;
    private final String recipientId;
    private final Long userId;

    public MessengerAccountConnectedEvent(Object source, String platform, String recipientId, Long userId) {
        super(source);
        this.platform = platform;
        this.recipientId = recipientId;
        this.userId = userId;
    }
}
