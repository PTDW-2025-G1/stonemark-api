package pt.estga.proposal.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pt.estga.proposal.entities.MarkOccurrenceProposal;

@Getter
public class ProposalPhotoUploadedEvent extends ApplicationEvent {

    private final MarkOccurrenceProposal proposal;

    public ProposalPhotoUploadedEvent(Object source, MarkOccurrenceProposal proposal) {
        super(source);
        this.proposal = proposal;
    }
}
