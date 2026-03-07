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

        // After displaying code, don't automatically transition - wait for user action
        if (currentState == VerificationState.DISPLAYING_VERIFICATION_CODE) {
            // User can navigate to main menu or other actions from here
            return CoreState.START;
        }

        // Handle branching from AWAITING_CONTACT
        if (currentState == VerificationState.AWAITING_CONTACT && outcome == SUCCESS) {
            if (context.getDomainUserId() != null) {
                return VerificationState.PHONE_CONNECTION_SUCCESS;
            }
            return VerificationState.PHONE_VERIFICATION_SUCCESS;
        }

        // Handle branching from AWAITING_PHONE_CONNECTION_DECISION
        if (currentState == VerificationState.AWAITING_PHONE_CONNECTION_DECISION) {
            if (outcome == VERIFY_WITH_PHONE) return VerificationState.AWAITING_CONTACT;
            if (outcome == SUCCESS) return CoreState.START;
        }

        if (outcome == SUCCESS) {
            return CoreState.START;
        }

        return currentState;
    }
}
