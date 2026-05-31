package pt.estga.chatbot.features.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.models.ui.Button;
import pt.estga.chatbot.models.ui.LocationRequest;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.services.FeatureHandler;
import pt.estga.chatbot.services.UiTextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static pt.estga.chatbot.services.ResponseFactory.menuResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmissionFeature implements FeatureHandler {

    private static final Map<ConversationState, ConversationState> SUCCESS_TRANSITIONS = Map.ofEntries(
            Map.entry(SubmissionState.WAITING_FOR_PHOTO, SubmissionState.AWAITING_LOCATION),
            Map.entry(SubmissionState.AWAITING_LOCATION, SubmissionState.AWAITING_NOTES),
            Map.entry(SubmissionState.AWAITING_NOTES, SubmissionState.SUBMITTED),
            Map.entry(SubmissionState.SUBMITTED, CoreState.MAIN_MENU)
    );

    private final UiTextService textService;
    private final MainMenuFactory mainMenuFactory;

    @Override
    public boolean supports(ConversationState state) {
        return state instanceof SubmissionState;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome) {
        if (outcome instanceof Failure) {
            return currentState;
        }

        if (currentState == SubmissionState.SUBMISSION_STATE) {
            return SubmissionState.WAITING_FOR_PHOTO;
        }

        if (outcome instanceof Success) {
            return SUCCESS_TRANSITIONS.getOrDefault(currentState, CoreState.START);
        }

        return currentState;
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
                RichText message = getEntryMessageForState(state);
                if (message == null) {
                    log.warn("Unhandled submission state: {}, returning empty response", state);
                    yield Collections.emptyList();
                }
                yield menuResponse(message);
            }
        };
    }

    @Override
    public RichText failureResponse(ChatbotContext context) {
        return switch ((SubmissionState) context.getCurrentState()) {
            case WAITING_FOR_PHOTO -> textService.get(MessageKey.EXPECTING_PHOTO_ERROR);
            case AWAITING_LOCATION -> textService.get(MessageKey.EXPECTING_LOCATION_ERROR);
            default -> null;
        };
    }

    private List<BotResponse> createNotesResponse() {
        Menu menu = Menu.builder()
                .titleNode(textService.get(MessageKey.ADD_NOTES_PROMPT))
                .buttons(List.of(
                        List.of(Button.builder()
                                .textNode(textService.get(MessageKey.SKIP_BTN))
                                .callbackData(CallbackData.SKIP_NOTES)
                                .build())
                ))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(menu).build());
    }

    private List<BotResponse> createLocationRequestResponse() {
        LocationRequest locationRequest = LocationRequest.builder()
                .messageNode(textService.get(MessageKey.REQUEST_LOCATION_PROMPT))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(locationRequest).build());
    }

    private List<BotResponse> createSubmissionSuccessResponse(BotInput input) {
        List<BotResponse> responses = new ArrayList<>();
        responses.addAll(menuResponse(textService.get(MessageKey.SUBMISSION_SUCCESS)));
        responses.add(BotResponse.builder().uiComponent(mainMenuFactory.create(input)).build());
        return responses;
    }

    private RichText getEntryMessageForState(SubmissionState state) {
        return switch (state) {
            case WAITING_FOR_PHOTO -> textService.get(MessageKey.REQUEST_PHOTO_PROMPT);
            case AWAITING_LOCATION -> textService.get(MessageKey.REQUEST_LOCATION_PROMPT);
            default -> null;
        };
    }
}
