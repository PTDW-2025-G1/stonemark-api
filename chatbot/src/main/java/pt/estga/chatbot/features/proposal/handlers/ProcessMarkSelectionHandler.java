package pt.estga.chatbot.features.proposal.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.features.proposal.ProposalCallbackData;
import pt.estga.chatbot.constants.SharedCallbackData;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.content.services.MarkQueryService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.Proposal;

import static pt.estga.chatbot.features.proposal.handlers.SelectMarkHandler.getHandlerOutcome;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessMarkSelectionHandler implements ConversationStateHandler {

    private final MarkQueryService markQueryService;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        String callbackData = input.getCallbackData();

        if (callbackData == null) {
            return HandlerOutcome.AWAITING_INPUT;
        }

        Proposal proposal = context.getProposalContext().getProposal();
        if (!(proposal instanceof MarkOccurrenceProposal markProposal)) {
            return HandlerOutcome.FAILURE;
        }

        if (callbackData.startsWith(ProposalCallbackData.PROPOSE_NEW_MARK)) {
            log.info("User proposed a new mark for proposal ID: {}", markProposal.getId());
            markProposal.setNewMark(true);
            markProposal.setExistingMark(null);
            return HandlerOutcome.SUCCESS;
        }

        if (callbackData.startsWith(ProposalCallbackData.CONFIRM_MARK_PREFIX)) {
            String[] callbackParts = callbackData.split(":");
            if (callbackParts.length < 2) {
                return HandlerOutcome.FAILURE;
            }

            boolean matches = SharedCallbackData.CONFIRM_YES.equalsIgnoreCase(callbackParts[1]);
            boolean rejected = SharedCallbackData.CONFIRM_NO.equalsIgnoreCase(callbackParts[1]);

            if (matches) {
                if (callbackParts.length < 3) {
                    return HandlerOutcome.FAILURE; // Mark ID is missing
                }
                try {
                    Long markId = Long.valueOf(callbackParts[2]);
                    log.info("User confirmed mark ID: {} for proposal ID: {}", markId, markProposal.getId());

                    // Load the full Mark entity to get all attributes
                    return getHandlerOutcome(markProposal, markId, markQueryService, log);
                } catch (NumberFormatException e) {
                    log.warn("Invalid mark ID in callback data: {}", callbackParts[2], e);
                    return HandlerOutcome.FAILURE;
                } catch (IllegalArgumentException e) {
                    log.warn("Mark not found: {}", e.getMessage());
                    return HandlerOutcome.FAILURE;
                }
            } else if (rejected) {
                log.info("User rejected suggested mark for proposal ID: {}", markProposal.getId());
                markProposal.setNewMark(true);
                markProposal.setExistingMark(null);
                return HandlerOutcome.REJECTED;
            }
        }
        return HandlerOutcome.FAILURE;
    }

    @Override
    public ConversationState canHandle() {
        return ProposalState.WAITING_FOR_MARK_CONFIRMATION;
    }
}
