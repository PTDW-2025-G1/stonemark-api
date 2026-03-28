package pt.estga.chatbot.features.submission.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.features.submission.SubmissionCallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.ChatbotSubmissionFacade;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddNotesHandler implements ConversationStateHandler {

    private final ChatbotSubmissionFacade submitFacade;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        MarkEvidenceSubmission submission = context.getSubmissionContext().getSubmission();
        if (!(submission instanceof MarkEvidenceSubmission markProposal)) {
            return HandlerOutcome.FAILURE;
        }

        // Handle "skip" or text input for notes
        if (input.getCallbackData() == null || !input.getCallbackData().equals(SubmissionCallbackData.SKIP_NOTES)) {
            if (input.getText() != null) {
                markProposal.setUserNotes(input.getText());
            }
        }

        try {
            // Delegate chatbot-specific orchestration to the facade which resolves user and source
            submitFacade.submitFromChatbot(
                    markProposal,
                    context.getSubmissionContext().getPhotoData(),
                    context.getSubmissionContext().getPhotoFilename(),
                    context.getDomainUserId(),
                    context.getSubmissionContext().getSubmissionSource()
            );

            // Clean up the context
            context.clear();

            return HandlerOutcome.SUCCESS;
        } catch (Exception e) {
            log.error("Failed to submit submission from chatbot", e);
            return HandlerOutcome.FAILURE;
        }
    }

    @Override
    public ConversationState canHandle() {
        return ProposalState.AWAITING_NOTES;
    }
}
