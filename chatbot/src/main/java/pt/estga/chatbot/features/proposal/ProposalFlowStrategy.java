package pt.estga.chatbot.features.proposal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.services.FlowStrategy;

import java.util.Map;

import static pt.estga.chatbot.context.HandlerOutcome.*;

@Component
@RequiredArgsConstructor
public class ProposalFlowStrategy implements FlowStrategy {

    private static final Map<ConversationState, ConversationState> SUCCESS_TRANSITIONS = Map.ofEntries(
            Map.entry(ProposalState.WAITING_FOR_PHOTO, ProposalState.AWAITING_LOCATION),
            Map.entry(ProposalState.AWAITING_LOCATION, ProposalState.AWAITING_NOTES),
            Map.entry(ProposalState.AWAITING_NOTES, ProposalState.SUBMITTED),
            Map.entry(ProposalState.SUBMITTED, CoreState.MAIN_MENU)
    );

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof ProposalState;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome) {
        if (outcome == FAILURE) {
            return currentState;
        }

        if (currentState == ProposalState.PROPOSAL_START) {
            return ProposalState.WAITING_FOR_PHOTO;
        }

        if (outcome == SUCCESS) {
            return SUCCESS_TRANSITIONS.getOrDefault(currentState, CoreState.START);
        }

        return currentState;
    }
}
