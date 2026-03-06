package pt.estga.chatbot.features.proposal.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.features.proposal.ProposalCallbackData;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.content.services.MarkQueryService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.Proposal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SelectMarkHandler implements ConversationStateHandler {

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

        if (callbackData.startsWith(ProposalCallbackData.SELECT_MARK_PREFIX)) {
            try {
                Long markId = Long.valueOf(callbackData.substring(ProposalCallbackData.SELECT_MARK_PREFIX.length()));
                log.info("User selected mark ID: {} for proposal ID: {}", markId, markProposal.getId());

                // Load the full Mark entity to get all attributes (description, etc.)
                return getHandlerOutcome(markProposal, markId, markQueryService, log);
            } catch (NumberFormatException e) {
                log.warn("Invalid mark ID in callback data: {}", callbackData, e);
                return HandlerOutcome.FAILURE;
            } catch (IllegalArgumentException e) {
                log.warn("Mark not found: {}", e.getMessage());
                return HandlerOutcome.FAILURE;
            }
        }

        if (callbackData.equals(ProposalCallbackData.PROPOSE_NEW_MARK)) {
            log.info("User chose to propose a new mark for proposal ID: {}", markProposal.getId());
            markProposal.setNewMark(true);
            markProposal.setExistingMark(null);
            return HandlerOutcome.PROPOSE_NEW;
        }

        return HandlerOutcome.FAILURE;
    }

    @NonNull
    static HandlerOutcome getHandlerOutcome(MarkOccurrenceProposal markProposal, Long markId, MarkQueryService markQueryService, Logger log) {
        markQueryService.findById(markId)
                .ifPresentOrElse(
                    mark -> {
                        markProposal.setExistingMark(mark);
                        markProposal.setNewMark(false);
                        log.info("Mark loaded and set successfully: {}", mark.getDescription());
                    },
                    () -> {
                        log.warn("Mark not found with ID: {}", markId);
                        throw new IllegalArgumentException("Mark not found with id: " + markId);
                    }
                );
        return HandlerOutcome.SUCCESS;
    }

    @Override
    public ConversationState canHandle() {
        return ProposalState.AWAITING_MARK_SELECTION;
    }
}

