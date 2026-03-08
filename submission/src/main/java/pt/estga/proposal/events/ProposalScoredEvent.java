package pt.estga.proposal.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProposalScoredEvent extends ApplicationEvent {
    private final Long proposalId;

    public ProposalScoredEvent(Object source, Long proposalId) {
        super(source);
        this.proposalId = proposalId;
    }
}
