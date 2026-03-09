package pt.estga.chatbot.features.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.VerificationState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.ui.ContactRequest;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.services.ResponseProvider;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pt.estga.chatbot.constants.EmojiKey.*;

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
                // Main text with instructions
                responses.add(buildSimpleMenuResponse(new Message(MessageKey.CONNECT_MESSENGER_INSTRUCTIONS, KEY)).getFirst());
                // Code in separate message
                String code = context.getVerificationCode();
                responses.add(BotResponse.builder()
                        .textNode(textService.get(new Message(MessageKey.CONNECT_MESSENGER_CODE, code)))
                        .build());
                // Show actionable menu right after code delivery so user can keep interacting.
                responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
                yield responses;
            }
            case AWAITING_CONTACT -> createContactRequestResponse();
        };
    }

    private List<BotResponse> createContactRequestResponse() {
        // Phone removed: instruct user to visit web UI and use the code to connect their messenger account.
        ContactRequest contactRequest = ContactRequest.builder()
                .messageNode(textService.get(new Message(MessageKey.CONNECT_MESSENGER_INSTRUCTIONS, KEY)))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(contactRequest).build());
    }

    private List<BotResponse> buildSimpleMenuResponse(Message message) {
        if (message == null) {
            return Collections.singletonList(BotResponse.builder().textNode(textService.get(new Message(MessageKey.ERROR_GENERIC, WARNING))).build());
        }
        return Collections.singletonList(BotResponse.builder()
                .uiComponent(Menu.builder().titleNode(textService.get(message)).build())
                .build());
    }
}
