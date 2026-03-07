package pt.estga.chatbot.features.verification.handlers;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;

@Component
public class PhoneConnectionSuccessHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        context.setCurrentState(CoreState.MAIN_MENU);
        return HandlerOutcome.RE_DISPATCH;
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.PHONE_CONNECTION_SUCCESS;
    }
}
