package pt.estga.chatbot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome.Failure;
import pt.estga.chatbot.context.HandlerOutcome;
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

        if (outcome instanceof Failure) {
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
        for (ResponseProvider provider : responseProviders) {
            if (provider.supports(context.getCurrentState())) {
                TextNode message = provider.failureResponse(context);
                if (message != null) {
                    return menuResponse(message);
                }
            }
        }
        return menuResponse(textService.get(MessageKey.ERROR_GENERIC));
    }
}
