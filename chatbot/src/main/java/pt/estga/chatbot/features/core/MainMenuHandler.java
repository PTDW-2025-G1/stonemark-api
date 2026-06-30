package pt.estga.chatbot.features.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.services.messages.UiTextService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MainMenuHandler implements ConversationStateHandler {

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String callbackData = input.getCallbackData();

        if (callbackData == null) {
            return HandlerOutcome.FAILURE;
        }

        if (callbackData.equals(CallbackData.START_SUBMISSION)) {
            return HandlerOutcome.SUCCESS;
        }

        if (callbackData.equals(CallbackData.START_VERIFICATION)) {
            return HandlerOutcome.SUCCESS;
        }

        return HandlerOutcome.FAILURE;
    }

    @Override
    public ConversationState canHandle() {
        return CoreState.MAIN_MENU;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        if (outcome == HandlerOutcome.FAILURE) {
            return currentState;
        }

        if (CallbackData.START_SUBMISSION.equals(input.getCallbackData())) {
            return SubmissionState.SUBMISSION_STATE;
        }
        if (CallbackData.START_VERIFICATION.equals(input.getCallbackData())) {
            return VerificationState.DISPLAYING_VERIFICATION_CODE;
        }

        return CoreState.START;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        List<BotResponse> responses = new ArrayList<>();
        if (context.getUserName() != null) {
            responses.add(BotResponse.builder().textNode(textService.get(MessageKey.WELCOME_BACK, context.getUserName())).build());
        } else {
            responses.add(BotResponse.builder().textNode(textService.get(MessageKey.WELCOME)).build());
        }
        responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
        return responses;
    }
}
