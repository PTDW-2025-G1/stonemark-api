package pt.estga.mark.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MarkOccurrenceCreatedEvent extends ApplicationEvent {
    private final Long occurrenceId;
    private final Long coverId;
    private final String filename;

    public MarkOccurrenceCreatedEvent(Object source, Long occurrenceId, Long coverId, String filename) {
        super(source);
        this.occurrenceId = occurrenceId;
        this.coverId = coverId;
        this.filename = filename;
    }
}
