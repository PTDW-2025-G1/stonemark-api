package pt.estga.user.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pt.estga.user.entities.User;

@Getter
public class EmailVerificationRequestedEvent extends ApplicationEvent {

    private final User user;
    private final String email;

    public EmailVerificationRequestedEvent(Object source, User user, String email) {
        super(source);
        this.user = user;
        this.email = email;
    }
}
