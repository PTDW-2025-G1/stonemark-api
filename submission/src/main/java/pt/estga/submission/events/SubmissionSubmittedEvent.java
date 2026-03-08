package pt.estga.submission.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubmissionSubmittedEvent extends ApplicationEvent {

    private final Long proposalId;

    public SubmissionSubmittedEvent(Object source, Long proposalId) {
        super(source);
        this.proposalId = proposalId;
    }
}
