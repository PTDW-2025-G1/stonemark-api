package pt.estga.chatbot.features.submission;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Message;
import pt.estga.chatbot.models.ui.Button;
import pt.estga.chatbot.models.ui.LocationRequest;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.services.ResponseProvider;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pt.estga.chatbot.constants.EmojiKey.ARROW_RIGHT;
import static pt.estga.chatbot.constants.EmojiKey.CAMERA;
import static pt.estga.chatbot.constants.EmojiKey.LOCATION;
import static pt.estga.chatbot.constants.EmojiKey.MEMO;
import static pt.estga.chatbot.constants.EmojiKey.PAPERCLIP;
import static pt.estga.chatbot.constants.EmojiKey.TADA;
import static pt.estga.chatbot.constants.EmojiKey.WARNING;

@Component
@RequiredArgsConstructor
public class SubmissionResponseProvider implements ResponseProvider {

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof SubmissionState;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        SubmissionState state = (SubmissionState) context.getCurrentState();
        return switch (state) {
            case SUBMISSION_STATE -> Collections.emptyList();
            case AWAITING_LOCATION -> createLocationRequestResponse();
            case AWAITING_NOTES -> createNotesResponse();
            case SUBMITTED -> createSubmissionSuccessResponse(input);
            default -> {
                Message message = getEntryMessageForState(state);
                yield buildSimpleMenuResponse(message);
            }
        };
    }

    private List<BotResponse> createNotesResponse() {
        Menu menu = Menu.builder()
                .titleNode(textService.get(new Message(MessageKey.ADD_NOTES_PROMPT, MEMO)))
                .buttons(List.of(
                        List.of(Button.builder()
                                .textNode(textService.get(new Message(MessageKey.SKIP_BTN, ARROW_RIGHT)))
                                .callbackData(SubmissionCallbackData.SKIP_NOTES)
                                .build())
                ))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(menu).build());
    }

    private List<BotResponse> createLocationRequestResponse() {
        LocationRequest locationRequest = LocationRequest.builder()
                .messageNode(textService.get(new Message(MessageKey.REQUEST_LOCATION_PROMPT, LOCATION, PAPERCLIP)))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(locationRequest).build());
    }

    private List<BotResponse> createSubmissionSuccessResponse(BotInput input) {
        List<BotResponse> responses = new ArrayList<>();
        responses.add(buildSimpleMenuResponse(new Message(MessageKey.SUBMISSION_SUCCESS, TADA)).getFirst());
        responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
        return responses;
    }

    private List<BotResponse> buildSimpleMenuResponse(Message message) {
        if (message == null) {
            return Collections.singletonList(BotResponse.builder().textNode(textService.get(new Message(MessageKey.ERROR_GENERIC, WARNING))).build());
        }
        return Collections.singletonList(BotResponse.builder()
                .uiComponent(Menu.builder().titleNode(textService.get(message)).build())
                .build());
    }

    private Message getEntryMessageForState(SubmissionState state) {
        return switch (state) {
            case WAITING_FOR_PHOTO -> new Message(MessageKey.REQUEST_PHOTO_PROMPT, CAMERA);
            case AWAITING_LOCATION -> new Message(MessageKey.REQUEST_LOCATION_PROMPT, LOCATION, PAPERCLIP);
            default -> null;
        };
    }
}
