package pt.estga.chatbot.features.core.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.features.submission.SubmissionCallbackData;
import pt.estga.chatbot.features.verification.VerificationCallbackData;
import pt.estga.chatbot.models.BotInput;

@Component
@RequiredArgsConstructor
public class MainMenuHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String callbackData = input.getCallbackData();

        if (callbackData == null) {
            return HandlerOutcome.FAILURE;
        }

        if (callbackData.equals(SubmissionCallbackData.START_SUBMISSION)) {
            return HandlerOutcome.START_NEW;
        }

        if (callbackData.equals(VerificationCallbackData.START_VERIFICATION)) {
            return HandlerOutcome.START_VERIFICATION;
        }

        return HandlerOutcome.FAILURE;
    }

    @Override
    public ConversationState canHandle() {
        return CoreState.MAIN_MENU;
    }
}
