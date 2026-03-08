package pt.estga.proposal.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pt.estga.proposal.entities.MarkOccurrenceProposal;

@Getter
public class ProposalAcceptedEvent extends ApplicationEvent {

    private final MarkOccurrenceProposal proposal;

    public ProposalAcceptedEvent(Object source, MarkOccurrenceProposal proposal) {
        super(source);
        this.proposal = proposal;
    }
}
