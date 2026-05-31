package pt.estga.chatbot.features.submission.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.services.ResponseFactory;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubmittedHandler implements ConversationStateHandler {

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        return new HandlerOutcome.Success();
    }

    @Override
    public ConversationState canHandle() {
        return SubmissionState.SUBMITTED;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        return CoreState.MAIN_MENU;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        List<BotResponse> responses = new ArrayList<>();
        responses.addAll(ResponseFactory.menuResponse(textService.get(MessageKey.SUBMISSION_SUCCESS)));
        responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
        return responses;
    }
}
