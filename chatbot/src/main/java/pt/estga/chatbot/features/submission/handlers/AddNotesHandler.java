package pt.estga.chatbot.features.submission.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.CallbackData;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.ui.Button;
import pt.estga.chatbot.models.ui.Menu;
import pt.estga.chatbot.services.UiTextService;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.ChatbotSubmissionFacade;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddNotesHandler implements ConversationStateHandler {

    private final ChatbotSubmissionFacade submitFacade;
    private final UiTextService textService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        MarkEvidenceSubmission submission = context.getSubmissionContext().getSubmission();
        if (!(submission instanceof MarkEvidenceSubmission markEvidenceSubmission)) {
            return new HandlerOutcome.Failure();
        }

        if (input.getCallbackData() == null || !input.getCallbackData().equals(CallbackData.SKIP_NOTES)) {
            if (input.getText() != null) {
                markEvidenceSubmission.setUserNotes(input.getText());
            }
        }

        try {
            submitFacade.submitFromChatbot(
                    markEvidenceSubmission,
                    context.getSubmissionContext().getStagedFileId(),
                    context.getSubmissionContext().getPhotoFilename(),
                    context.getDomainUserId(),
                    context.getSubmissionContext().getSubmissionSource()
            );

            context.clear();

            return new HandlerOutcome.Success();
        } catch (Exception e) {
            log.error("Failed to submit submission from chatbot", e);
            return new HandlerOutcome.Failure();
        }
    }

    @Override
    public ConversationState canHandle() {
        return SubmissionState.AWAITING_NOTES;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        if (outcome instanceof HandlerOutcome.Failure) {
            return currentState;
        }
        return SubmissionState.SUBMITTED;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
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
}
