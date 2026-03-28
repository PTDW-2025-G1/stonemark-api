package pt.estga.chatbot.features.submission.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.Platform;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.enums.SubmissionSource;

@Component
@RequiredArgsConstructor
public class InitialPhotoHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getType() != BotInput.InputType.PHOTO || input.getFileData() == null) {
            return HandlerOutcome.FAILURE;
        }

        MarkEvidenceSubmission submission = context.getSubmissionContext().getSubmission();

        // Initialize a submission object if not exists (but don't persist yet)
        if (submission == null) {
            MarkEvidenceSubmission markEvidenceSubmission = new MarkEvidenceSubmission();
            context.getSubmissionContext().setSubmission(markEvidenceSubmission);
        }

        // Store photo data and metadata in context (will be persisted at submission)
        context.getSubmissionContext().setPhotoData(input.getFileData());
        context.getSubmissionContext().setPhotoFilename(input.getFileName());
        context.getSubmissionContext().setSubmissionSource(mapPlatformToSubmissionSource(input.getPlatform()));

        return HandlerOutcome.SUCCESS;
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
