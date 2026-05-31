package pt.estga.chatbot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.TextNode;
import pt.estga.chatbot.models.ui.Menu;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponseFactory {

    private final List<ResponseProvider> responseProviders;
    private final UiTextService textService;

    public static List<BotResponse> menuResponse(TextNode titleNode) {
        return Collections.singletonList(BotResponse.builder()
                .uiComponent(Menu.builder().titleNode(titleNode).build())
                .build());
    }

    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        ConversationState currentState = context.getCurrentState();

        if (outcome == HandlerOutcome.FAILURE) {
            return createErrorResponse(context);
        }

        for (ResponseProvider provider : responseProviders) {
            if (provider.supports(currentState)) {
                return provider.createResponse(context, outcome, input);
            }
        }

        return createErrorResponse(context);
    }

    public List<BotResponse> createErrorResponse(ChatbotContext context) {
        TextNode message = resolveFailureMessage(context.getCurrentState());
        if (message == null) {
            return menuResponse(textService.get(MessageKey.ERROR_GENERIC));
        }
        return menuResponse(message);
    }

    private TextNode resolveFailureMessage(ConversationState state) {
        if (state instanceof SubmissionState) {
            return switch ((SubmissionState) state) {
                case WAITING_FOR_PHOTO -> textService.get(MessageKey.EXPECTING_PHOTO_ERROR);
                case AWAITING_LOCATION -> textService.get(MessageKey.EXPECTING_LOCATION_ERROR);
                default -> null;
            };
        } else if (state instanceof VerificationState) {
            return textService.get(MessageKey.ERROR_GENERIC);
        }
        return null;
    }
}
