package pt.estga.verification.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MessengerAccountConnectedEvent extends ApplicationEvent {

    private final String telegramId;
    private final Long userId;

    public MessengerAccountConnectedEvent(Object source, String telegramId, Long userId) {
        super(source);
        this.telegramId = telegramId;
        this.userId = userId;
    }
}
