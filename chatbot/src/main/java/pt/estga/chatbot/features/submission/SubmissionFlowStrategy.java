package pt.estga.chatbot.features.submission;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.HandlerOutcome.Failure;
import pt.estga.chatbot.context.HandlerOutcome.Success;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.services.FlowStrategy;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubmissionFlowStrategy implements FlowStrategy {

    private static final Map<ConversationState, ConversationState> SUCCESS_TRANSITIONS = Map.ofEntries(
            Map.entry(SubmissionState.WAITING_FOR_PHOTO, SubmissionState.AWAITING_LOCATION),
            Map.entry(SubmissionState.AWAITING_LOCATION, SubmissionState.AWAITING_NOTES),
            Map.entry(SubmissionState.AWAITING_NOTES, SubmissionState.SUBMITTED),
            Map.entry(SubmissionState.SUBMITTED, CoreState.MAIN_MENU)
    );

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof SubmissionState;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome) {
        if (outcome instanceof Failure) {
            return currentState;
        }

        if (currentState == SubmissionState.SUBMISSION_STATE) {
            return SubmissionState.WAITING_FOR_PHOTO;
        }

        if (outcome instanceof Success) {
            return SUCCESS_TRANSITIONS.getOrDefault(currentState, CoreState.START);
        }

        return currentState;
    }
}
