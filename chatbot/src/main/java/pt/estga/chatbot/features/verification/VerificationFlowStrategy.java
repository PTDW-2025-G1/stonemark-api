package pt.estga.chatbot.features.verification;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.services.FlowStrategy;

import static pt.estga.chatbot.context.HandlerOutcome.*;

@Component
public class VerificationFlowStrategy implements FlowStrategy {

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof VerificationState;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome) {
        if (outcome == FAILURE) {
            return currentState;
        }

        // After displaying code, go back to main start
        if (currentState == VerificationState.DISPLAYING_VERIFICATION_CODE) {
            return CoreState.START;
        }

        // If awaiting contact and success, return to main start
        if (currentState == VerificationState.AWAITING_CONTACT && outcome == SUCCESS) {
            return CoreState.START;
        }

        if (outcome == SUCCESS) {
            return CoreState.START;
        }

        return currentState;
    }
}
