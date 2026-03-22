package pt.estga.mark.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MarkCreatedEvent extends ApplicationEvent {
    private final Long markId;
    private final Long coverId;
    private final String filename;

    public MarkCreatedEvent(Object source, Long markId, Long coverId, String filename) {
        super(source);
        this.markId = markId;
        this.coverId = coverId;
        this.filename = filename;
    }
}
