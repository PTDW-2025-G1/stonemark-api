package pt.estga.chatbot.features.submission;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.models.ui.LocationRequest;
import pt.estga.chatbot.services.messages.UiTextService;
import pt.estga.intake.entities.MarkEvidenceSubmission;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InitialLocationHandler implements ConversationStateHandler {

    private final UiTextService textService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getLocation() == null) {
            return new HandlerOutcome.Failure();
        }

        MarkEvidenceSubmission submission = context.getSubmission();
        if (!(submission instanceof MarkEvidenceSubmission markEvidenceSubmission)) {
            return new HandlerOutcome.Failure();
        }

        markEvidenceSubmission.setLatitude(input.getLocation().getLatitude());
        markEvidenceSubmission.setLongitude(input.getLocation().getLongitude());

        return new HandlerOutcome.Success();
    }

    @Override
    public ConversationState canHandle() {
        return SubmissionState.AWAITING_LOCATION;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        if (outcome instanceof HandlerOutcome.Failure) {
            return currentState;
        }
        return SubmissionState.AWAITING_NOTES;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        LocationRequest locationRequest = LocationRequest.builder()
                .messageNode(textService.get(MessageKey.REQUEST_LOCATION_PROMPT))
                .build();
        return Collections.singletonList(BotResponse.builder().uiComponent(locationRequest).build());
    }

    @Override
    public RichText failureResponse(ChatbotContext context) {
        return textService.get(MessageKey.EXPECTING_LOCATION_ERROR);
    }
}
