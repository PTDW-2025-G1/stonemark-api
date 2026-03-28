package pt.estga.intake.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MarkEvidenceSubmittedEvent extends ApplicationEvent {

    private final Long submissionId;

    public MarkEvidenceSubmittedEvent(Object source, Long submissionId) {
        super(source);
        this.submissionId = submissionId;
    }
}
