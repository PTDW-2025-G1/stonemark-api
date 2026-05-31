package pt.estga.chatbot.features.core.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.models.BotInput;

@Component
@RequiredArgsConstructor
public class MainMenuHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String callbackData = input.getCallbackData();

        if (callbackData == null) {
            return new HandlerOutcome.Failure();
        }

        if (callbackData.equals(CallbackData.START_SUBMISSION)) {
            return new HandlerOutcome.StartNew();
        }

        if (callbackData.equals(CallbackData.START_VERIFICATION)) {
            return new HandlerOutcome.StartVerification();
        }

        return new HandlerOutcome.Failure();
    }

    @Override
    public ConversationState canHandle() {
        return CoreState.MAIN_MENU;
    }
}
