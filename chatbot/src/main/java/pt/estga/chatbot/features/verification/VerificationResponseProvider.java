package pt.estga.chatbot.features.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.TextNode;
import pt.estga.chatbot.services.ResponseProvider;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.List;

import static pt.estga.chatbot.services.ResponseFactory.menuResponse;

@Component
@RequiredArgsConstructor
public class VerificationResponseProvider implements ResponseProvider {

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof VerificationState;
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
