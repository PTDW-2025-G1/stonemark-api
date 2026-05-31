package pt.estga.chatbot.services;

import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.RichText;

import java.util.List;

public interface FeatureHandler {
    boolean supports(ConversationState state);
    ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input);
    List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input);

    default RichText failureResponse(ChatbotContext context) {
        return null;
    }
}
