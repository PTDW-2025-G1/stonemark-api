package pt.estga.chatbot.features.verification.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.features.verification.VerificationCallbackData;

@Component
@RequiredArgsConstructor
public class ChooseVerificationMethodHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String callbackData = input.getCallbackData();

        if (callbackData == null) {
            return HandlerOutcome.AWAITING_INPUT;
        }

        if (callbackData.equals(VerificationCallbackData.CHOOSE_VERIFY_WITH_CODE)) {
            return HandlerOutcome.VERIFY_WITH_CODE;
        }

        if (callbackData.equals(VerificationCallbackData.CHOOSE_VERIFY_WITH_PHONE)) {
            return HandlerOutcome.VERIFY_WITH_PHONE;
        }

        return HandlerOutcome.FAILURE;
    }

    @Override
    public ConversationState canHandle() {
        return VerificationState.AWAITING_VERIFICATION_METHOD;
    }
}
