package pt.estga.chatbot.features.proposal.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.features.proposal.ProposalCallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.submission.entities.MarkEvidenceSubmission;
import pt.estga.submission.services.SubmissionService;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddNotesHandler implements ConversationStateHandler {

    private final SubmissionService submitService;
    private final UserService userService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        MarkEvidenceSubmission submission = context.getProposalContext().getSubmission();
        if (!(submission instanceof MarkEvidenceSubmission markProposal)) {
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

            // Attach optional user and source on the submission object (preserve existing values)
            if (user != null && markProposal.getSubmittedBy() == null) {
                markProposal.setSubmittedBy(user);
            }
            if (context.getProposalContext().getSubmissionSource() != null && markProposal.getSubmissionSource() == null) {
                markProposal.setSubmissionSource(context.getProposalContext().getSubmissionSource());
            }

            // Submit the submission using the simple 3-argument API: submission already contains user/source
            submitService.submit(
                    markProposal,
                    context.getProposalContext().getPhotoData(),
                    context.getProposalContext().getPhotoFilename()
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
