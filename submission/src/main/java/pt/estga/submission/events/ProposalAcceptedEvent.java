package pt.estga.submission.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pt.estga.submission.entities.MarkOccurrenceSubmission;

@Getter
public class ProposalAcceptedEvent extends ApplicationEvent {

    private final MarkOccurrenceSubmission proposal;

    public ProposalAcceptedEvent(Object source, MarkOccurrenceSubmission proposal) {
        super(source);
        this.proposal = proposal;
    }
}
