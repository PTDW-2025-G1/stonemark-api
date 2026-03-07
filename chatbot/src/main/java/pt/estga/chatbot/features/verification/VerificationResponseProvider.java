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
import pt.estga.chatbot.models.ui.Button;
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
                responses.add(buildSimpleMenuResponse(new Message(MessageKey.DISPLAY_VERIFICATION_CODE_TITLE, KEY)).getFirst());
                // Code in separate message
                String code = context.getVerificationCode();
                responses.add(BotResponse.builder()
                        .textNode(textService.get(new Message(MessageKey.DISPLAY_VERIFICATION_CODE, code)))
                        .build());
                yield responses;
            }
            case AWAITING_CONTACT -> createContactRequestResponse();
            case AWAITING_PHONE_CONNECTION_DECISION -> {
                List<BotResponse> responses = new ArrayList<>();
                responses.add(buildSimpleMenuResponse(new Message(MessageKey.VERIFICATION_SUCCESS_CODE, context.getUserName(), TADA)).getFirst());
                responses.add(createPhoneConnectionPrompt().getFirst());
                yield responses;
            }
            case PHONE_VERIFICATION_SUCCESS -> {
                List<BotResponse> responses = new ArrayList<>();
                responses.add(buildSimpleMenuResponse(new Message(MessageKey.VERIFICATION_SUCCESS_PHONE, TADA)).getFirst());
                responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
                yield responses;
            }
            case PHONE_CONNECTION_SUCCESS -> {
                List<BotResponse> responses = new ArrayList<>();
                responses.add(buildSimpleMenuResponse(new Message(MessageKey.PHONE_CONNECTION_SUCCESS, TADA)).getFirst());
                responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
                yield responses;
            }
        };
    }


    private List<BotResponse> createContactRequestResponse() {
        ContactRequest contactRequest = ContactRequest.builder()
                .messageNode(textService.get(new Message(MessageKey.SHARE_PHONE_NUMBER_PROMPT, PHONE)))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(contactRequest).build());
    }

    private List<BotResponse> createPhoneConnectionPrompt() {
        Menu menu = Menu.builder()
                .titleNode(textService.get(new Message(MessageKey.PROMPT_CONNECT_PHONE)))
                .buttons(List.of(
                        List.of(Button.builder().textNode(textService.get(new Message(MessageKey.YES_BTN, CHECK))).callbackData(VerificationCallbackData.CONNECT_PHONE_YES).build()),
                        List.of(Button.builder().textNode(textService.get(new Message(MessageKey.NO_BTN, CROSS))).callbackData(VerificationCallbackData.CONNECT_PHONE_NO).build())
                ))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(menu).build());
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
