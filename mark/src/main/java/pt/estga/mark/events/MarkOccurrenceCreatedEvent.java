package pt.estga.mark.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class MarkOccurrenceCreatedEvent extends ApplicationEvent {
    private final Long occurrenceId;
    private final UUID coverId;
    private final String filename;

    public MarkOccurrenceCreatedEvent(Object source, Long occurrenceId, UUID coverId, String filename) {
        super(source);
        this.occurrenceId = occurrenceId;
        this.coverId = coverId;
        this.filename = filename;
    }
}
