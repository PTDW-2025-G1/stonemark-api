package pt.estga.chatbot.features.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.HandlerOutcome.Failure;
import pt.estga.chatbot.context.HandlerOutcome.Success;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.services.FeatureHandler;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CoreFeature implements FeatureHandler {

    private static final Map<ConversationState, ConversationState> SUCCESS_TRANSITIONS = Map.ofEntries(
            Map.entry(CoreState.START, CoreState.MAIN_MENU)
    );

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof CoreState;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        if (outcome instanceof Failure) {
            return currentState;
        }

        if (currentState == CoreState.MAIN_MENU) {
            if (CallbackData.START_SUBMISSION.equals(input.getCallbackData())) return SubmissionState.SUBMISSION_STATE;
            if (CallbackData.START_VERIFICATION.equals(input.getCallbackData())) return VerificationState.DISPLAYING_VERIFICATION_CODE;
        }

        if (outcome instanceof Success) {
            return SUCCESS_TRANSITIONS.getOrDefault(currentState, CoreState.START);
        }

        return currentState;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        CoreState state = (CoreState) context.getCurrentState();
        return switch (state) {
            case MAIN_MENU -> {
                List<BotResponse> responses = new ArrayList<>();
                if (context.getUserName() != null) {
                    responses.add(BotResponse.builder().textNode(textService.get(MessageKey.WELCOME_BACK, context.getUserName())).build());
                } else {
                    responses.add(BotResponse.builder().textNode(textService.get(MessageKey.WELCOME)).build());
                }
                responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
                yield responses;
            }
            default -> Collections.emptyList();
        };
    }
}
