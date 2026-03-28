package pt.estga.chatbot.features.core;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.services.FlowStrategy;

import java.util.Map;

import static pt.estga.chatbot.context.HandlerOutcome.*;

@Component
public class CoreFlowStrategy implements FlowStrategy {

    private static final Map<ConversationState, ConversationState> SUCCESS_TRANSITIONS = Map.ofEntries(
            Map.entry(CoreState.START, CoreState.MAIN_MENU)
    );

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof CoreState;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome) {
        if (outcome == FAILURE) {
            return currentState;
        }

        // Handle branching from MAIN_MENU state
        if (currentState == CoreState.MAIN_MENU) {
            if (outcome == START_NEW) return SubmissionState.SUBMISSION_STATE;
            if (outcome == START_VERIFICATION) return VerificationState.DISPLAYING_VERIFICATION_CODE;
        }

        if (outcome == SUCCESS) {
            return SUCCESS_TRANSITIONS.getOrDefault(currentState, CoreState.START);
        }

        return currentState;
    }
}
