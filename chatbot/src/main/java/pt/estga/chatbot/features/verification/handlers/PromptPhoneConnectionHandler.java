package pt.estga.chatbot.features.verification.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.features.verification.VerificationCallbackData;
import pt.estga.chatbot.models.BotInput;

@Component
@RequiredArgsConstructor
public class PromptPhoneConnectionHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String callbackData = input.getCallbackData();

        if (callbackData == null) {
            return HandlerOutcome.FAILURE;
        }

        if (callbackData.equals(VerificationCallbackData.CONNECT_PHONE_YES)) {
            return HandlerOutcome.VERIFY_WITH_PHONE;
        } else if (callbackData.equals(VerificationCallbackData.CONNECT_PHONE_NO)) {
            return HandlerOutcome.SUCCESS;
        } else {
            return HandlerOutcome.FAILURE;
        }
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.AWAITING_PHONE_CONNECTION_DECISION;
    }
}
