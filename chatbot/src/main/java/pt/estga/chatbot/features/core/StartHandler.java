package pt.estga.chatbot.features.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        context.setCurrentState(CoreState.MAIN_MENU);
        return new HandlerOutcome.Success();
    }

    @Override
    public ConversationState canHandle() {
        return CoreState.START;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        if (outcome instanceof HandlerOutcome.Failure) {
            return currentState;
        }
        return CoreState.MAIN_MENU;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        return Collections.emptyList();
    }
}
