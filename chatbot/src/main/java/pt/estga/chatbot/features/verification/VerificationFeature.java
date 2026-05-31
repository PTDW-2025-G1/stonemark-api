package pt.estga.chatbot.features.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.HandlerOutcome.Failure;
import pt.estga.chatbot.context.HandlerOutcome.Success;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.TextNode;
import pt.estga.chatbot.services.FeatureHandler;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.List;

import static pt.estga.chatbot.services.ResponseFactory.menuResponse;

@Component
@RequiredArgsConstructor
public class VerificationFeature implements FeatureHandler {

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof VerificationState;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome) {
        if (outcome instanceof Failure) {
            return currentState;
        }

        if (currentState == VerificationState.DISPLAYING_VERIFICATION_CODE) {
            return CoreState.START;
        }

        if (outcome instanceof Success) {
            return CoreState.START;
        }

        return currentState;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        VerificationState state = (VerificationState) context.getCurrentState();
        return switch (state) {
            case DISPLAYING_VERIFICATION_CODE -> {
                List<BotResponse> responses = new ArrayList<>();
                TextNode instructions = textService.get(MessageKey.CONNECT_MESSENGER_INSTRUCTIONS);
                responses.addAll(menuResponse(instructions));
                String code = context.getVerificationCode();
                responses.add(BotResponse.builder()
                        .textNode(textService.get(MessageKey.CONNECT_MESSENGER_CODE, code))
                        .build());
                responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
                yield responses;
            }
        };
    }
}
