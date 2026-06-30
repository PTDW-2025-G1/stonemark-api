package pt.estga.chatbot.features.submission;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;

import java.util.Collections;
import java.util.List;

@Component
public class SubmissionStartHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        return HandlerOutcome.REDISPATCH;
    }

    @Override
    public ConversationState canHandle() {
        return SubmissionState.SUBMISSION_STATE;
    }

    @Override
    public boolean isAutomatic() {
        return true;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        return SubmissionState.WAITING_FOR_PHOTO;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        return Collections.emptyList();
    }
}
