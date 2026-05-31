package pt.estga.chatbot.features.core;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.HandlerOutcome.Failure;
import pt.estga.chatbot.context.HandlerOutcome.Success;
import pt.estga.chatbot.context.HandlerOutcome.StartNew;
import pt.estga.chatbot.context.HandlerOutcome.StartVerification;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.services.FlowStrategy;

import java.util.Map;

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
        if (outcome instanceof Failure) {
            return currentState;
        }

        if (currentState == CoreState.MAIN_MENU) {
            if (outcome instanceof StartNew) return SubmissionState.SUBMISSION_STATE;
            if (outcome instanceof StartVerification) return VerificationState.DISPLAYING_VERIFICATION_CODE;
        }

        if (outcome instanceof Success) {
            return SUCCESS_TRANSITIONS.getOrDefault(currentState, CoreState.START);
        }

        return currentState;
    }
}
