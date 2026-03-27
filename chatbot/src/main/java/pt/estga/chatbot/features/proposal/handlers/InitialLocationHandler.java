package pt.estga.chatbot.features.proposal.handlers;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.intake.entities.MarkEvidenceSubmission;

@Component
public class InitialLocationHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getLocation() == null) {
            return HandlerOutcome.FAILURE;
        }

        MarkEvidenceSubmission submission = context.getProposalContext().getSubmission();
        if (!(submission instanceof MarkEvidenceSubmission markProposal)) {
            return HandlerOutcome.FAILURE;
        }

        markProposal.setLatitude(input.getLocation().getLatitude());
        markProposal.setLongitude(input.getLocation().getLongitude());

        // Flow strategy advances directly to optional notes.
        return HandlerOutcome.SUCCESS;
    }

    @Override
    public ConversationState canHandle() {
        return ProposalState.AWAITING_LOCATION;
    }
}
