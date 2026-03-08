package pt.estga.submission.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubmissionScoredEvent extends ApplicationEvent {
    private final Long proposalId;

    public SubmissionScoredEvent(Object source, Long proposalId) {
        super(source);
        this.proposalId = proposalId;
    }
}
