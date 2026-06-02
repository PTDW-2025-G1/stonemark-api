package pt.estga.chatbot.features.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.services.messages.ResponseFactory;
import pt.estga.chatbot.services.messages.UiTextService;
import pt.estga.fileapi.FileStorageOperations;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class InitialPhotoHandler implements ConversationStateHandler {

    private final FileStorageOperations fileStorage;
    private final UiTextService textService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getType() != BotInput.InputType.PHOTO || input.getFileData() == null) {
            return new HandlerOutcome.Failure();
        }

        MarkEvidenceSubmission submission = context.getSubmission();

        if (submission == null) {
            submission = new MarkEvidenceSubmission();
            context.setSubmission(submission);
        }

        try {
            var staged = fileStorage.stage(new ByteArrayInputStream(input.getFileData()), input.getFileName());
            context.getSubmissionContext().setStagedFileId(staged.id());
            context.getSubmissionContext().setPhotoFilename(input.getFileName());
            context.getSubmissionContext().setSubmissionSource(mapPlatformToSubmissionSource(input.getPlatform()));
            log.debug("Staged photo {} as {}", input.getFileName(), staged.id());
        } catch (Exception e) {
            log.error("Failed to stage photo from chat {}", input.getChatId(), e);
            return new HandlerOutcome.Failure();
        }

        return new HandlerOutcome.Success();
    }

    @Override
    public ConversationState canHandle() {
        return SubmissionState.WAITING_FOR_PHOTO;
    }

    @Override
    public ConversationState getNextState(ChatbotContext context, ConversationState currentState, HandlerOutcome outcome, BotInput input) {
        if (outcome instanceof HandlerOutcome.Failure) {
            return currentState;
        }
        return SubmissionState.AWAITING_LOCATION;
    }

    @Override
    public List<BotResponse> createResponse(ChatbotContext context, HandlerOutcome outcome, BotInput input) {
        return ResponseFactory.menuResponse(textService.get(MessageKey.REQUEST_PHOTO_PROMPT));
    }

    @Override
    public RichText failureResponse(ChatbotContext context) {
        return textService.get(MessageKey.EXPECTING_PHOTO_ERROR);
    }

    private SubmissionSource mapPlatformToSubmissionSource(Platform platform) {
        if (platform == null) {
            return SubmissionSource.OTHER;
        }
        return switch (platform) {
            case TELEGRAM -> SubmissionSource.TELEGRAM_BOT;
            case WHATSAPP -> SubmissionSource.WHATSAPP;
            default -> SubmissionSource.OTHER;
        };
    }
}
