package pt.estga.chatbot.features.proposal.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.Platform;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.entities.Submission;
import pt.estga.submission.enums.SubmissionSource;

@Component
@RequiredArgsConstructor
public class InitialPhotoHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getType() != BotInput.InputType.PHOTO || input.getFileData() == null) {
            return HandlerOutcome.FAILURE;
        }

        Submission submission = context.getProposalContext().getSubmission();

        // Initialize a submission object if not exists (but don't persist yet)
        if (submission == null) {
            MarkOccurrenceSubmission markProposal = new MarkOccurrenceSubmission();
            context.getProposalContext().setSubmission(markProposal);
        } else if (!(submission instanceof MarkOccurrenceSubmission)) {
            return HandlerOutcome.FAILURE;
        }

        // Store photo data and metadata in context (will be persisted at submission)
        context.getProposalContext().setPhotoData(input.getFileData());
        context.getProposalContext().setPhotoFilename(input.getFileName());
        context.getProposalContext().setSubmissionSource(mapPlatformToSubmissionSource(input.getPlatform()));

        return HandlerOutcome.SUCCESS;
    }

    @Override
    public ConversationState canHandle() {
        return ProposalState.WAITING_FOR_PHOTO;
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
