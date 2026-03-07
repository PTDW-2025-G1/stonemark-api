package pt.estga.chatbot.features.proposal.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.*;
import pt.estga.chatbot.features.proposal.ProposalCallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.services.chatbot.ProposalChatbotSubmitService;
import pt.estga.user.entities.User;
import pt.estga.user.services.UserService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddNotesHandler implements ConversationStateHandler {

    private final ProposalChatbotSubmitService chatbotSubmitService;
    private final UserService userService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        Proposal proposal = context.getProposalContext().getProposal();
        if (!(proposal instanceof MarkOccurrenceProposal markProposal)) {
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

            // Submit the proposal with all collected data (authenticated or anonymous)
            chatbotSubmitService.submitFromChatbot(
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
            log.error("Failed to submit proposal from chatbot", e);
            return HandlerOutcome.FAILURE;
        }
    }

    @Override
    public ConversationState canHandle() {
        return ProposalState.AWAITING_NOTES;
    }
}
