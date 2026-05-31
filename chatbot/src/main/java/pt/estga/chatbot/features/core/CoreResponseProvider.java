package pt.estga.chatbot.features.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.services.ResponseProvider;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CoreResponseProvider implements ResponseProvider {

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof CoreState;
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
