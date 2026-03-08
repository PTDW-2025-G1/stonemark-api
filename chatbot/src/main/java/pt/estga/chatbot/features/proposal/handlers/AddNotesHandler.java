package pt.estga.chatbot.features.proposal.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.features.proposal.ProposalCallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.submission.entities.MarkOccurrenceSubmission;
import pt.estga.submission.services.MarkOccurrenceSubmissionSubmitService;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddNotesHandler implements ConversationStateHandler {

    private final MarkOccurrenceSubmissionSubmitService submitService;
    private final UserService userService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        MarkOccurrenceSubmission submission = context.getProposalContext().getSubmission();
        if (!(submission instanceof MarkOccurrenceSubmission markProposal)) {
            return HandlerOutcome.FAILURE;
        }

        // Handle "skip" or text input for notes
        if (input.getCallbackData() == null || !input.getCallbackData().equals(ProposalCallbackData.SKIP_NOTES)) {
            if (input.getText() != null) {
                markProposal.setUserNotes(input.getText());
            }
        }

        try {
            Long domainUserId = context.getDomainUserId();
            User user = domainUserId != null
                    ? userService.findById(domainUserId).orElse(null)
                    : null;

            // Submit the submission with all collected data (authenticated or anonymous)
            submitService.submitFromChatbot(
                    markProposal,
                    context.getProposalContext().getPhotoData(),
                    context.getProposalContext().getPhotoFilename(),
                    user,
                    context.getProposalContext().getSubmissionSource()
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
