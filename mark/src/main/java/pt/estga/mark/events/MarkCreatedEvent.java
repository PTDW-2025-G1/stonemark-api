package pt.estga.mark.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class MarkCreatedEvent extends ApplicationEvent {
    private final Long markId;
    private final UUID coverId;
    private final String filename;

    public MarkCreatedEvent(Object source, Long markId, UUID coverId, String filename) {
        super(source);
        this.markId = markId;
        this.coverId = coverId;
        this.filename = filename;
    }
}
