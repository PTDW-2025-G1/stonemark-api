package pt.estga.chatbot.features.submission.handlers;

import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.intake.entities.MarkEvidenceSubmission;

@Component
public class InitialLocationHandler implements ConversationStateHandler {

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        if (input.getLocation() == null) {
            return HandlerOutcome.FAILURE;
        }

        MarkEvidenceSubmission submission = context.getSubmissionContext().getSubmission();
        if (!(submission instanceof MarkEvidenceSubmission markEvidenceSubmission)) {
            return HandlerOutcome.FAILURE;
        }

        markEvidenceSubmission.setLatitude(input.getLocation().getLatitude());
        markEvidenceSubmission.setLongitude(input.getLocation().getLongitude());

        // Flow strategy advances directly to optional notes.
        return HandlerOutcome.SUCCESS;
    }

    @Override
    public ConversationState canHandle() {
        return SubmissionState.AWAITING_LOCATION;
    }
}
