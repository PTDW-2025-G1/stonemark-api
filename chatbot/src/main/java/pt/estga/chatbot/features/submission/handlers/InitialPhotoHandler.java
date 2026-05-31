package pt.estga.chatbot.features.submission.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.Platform;
import pt.estga.fileapi.FileStorageOperations;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;

import java.io.ByteArrayInputStream;

@Component
@Slf4j
@RequiredArgsConstructor
public class InitialPhotoHandler implements ConversationStateHandler {

    private final FileStorageOperations fileStorage;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getType() != BotInput.InputType.PHOTO || input.getFileData() == null) {
            return new HandlerOutcome.Failure();
        }

        MarkEvidenceSubmission submission = context.getSubmissionContext().getSubmission();

        if (submission == null) {
            submission = new MarkEvidenceSubmission();
            context.getSubmissionContext().setSubmission(submission);
        }

        // Stage the photo immediately to avoid keeping large byte[] in memory
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
