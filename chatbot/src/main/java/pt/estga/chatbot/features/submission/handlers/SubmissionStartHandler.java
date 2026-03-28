package pt.estga.chatbot.features.submission.handlers;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.models.BotInput;

@Component
public class SubmissionStartHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        return HandlerOutcome.RE_DISPATCH;
    }

    @Override
    public ConversationState canHandle() {
        return SubmissionState.SUBMISSION_STATE;
    }

    @Override
    public boolean isAutomatic() {
        return true;
    }
}
