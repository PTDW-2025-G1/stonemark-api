package pt.estga.chatbot.features.core.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.models.BotInput;

@Component
@RequiredArgsConstructor
public class StartHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        context.setCurrentState(CoreState.MAIN_MENU);
        return HandlerOutcome.SUCCESS;
    }

    @Override
    public ConversationState canHandle() {
        return CoreState.START;
    }
}
