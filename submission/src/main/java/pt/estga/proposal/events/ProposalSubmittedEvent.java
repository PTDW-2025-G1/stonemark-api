package pt.estga.proposal.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProposalSubmittedEvent extends ApplicationEvent {

    private final Long proposalId;

    public ProposalSubmittedEvent(Object source, Long proposalId) {
        super(source);
        this.proposalId = proposalId;
    }
}
