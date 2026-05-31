package pt.estga.chatbot.context;

import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.RichText;

import java.util.List;

public interface ConversationStateHandler {

    HandlerOutcome handle(ChatbotContext context, BotInput input);

    ConversationState canHandle();

    ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input);

    List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input);

    default boolean isAutomatic() {
        return false;
    }

    default RichText failureResponse(ChatbotContext context) {
        return null;
    }
}
