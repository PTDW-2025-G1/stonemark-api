package pt.estga.user.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pt.estga.user.entities.User;

@Getter
public class TelephoneVerificationRequestedEvent extends ApplicationEvent {

    private final User user;
    private final String phone;

    public TelephoneVerificationRequestedEvent(Object source, User user, String phone) {
        super(source);
        this.user = user;
        this.phone = phone;
    }
}

