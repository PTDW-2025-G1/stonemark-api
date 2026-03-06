package pt.estga.chatbot.features.proposal.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.content.entities.Mark;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.Proposal;
import pt.estga.proposal.repositories.MarkOccurrenceProposalRepository;
import pt.estga.proposal.services.chatbot.MarkOccurrenceProposalChatbotFlowService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoAnalysisHandler implements ConversationStateHandler {

    private final MarkOccurrenceProposalChatbotFlowService proposalFlowService;
    private final MarkOccurrenceProposalRepository proposalRepository;

    @Override
    public HandlerOutcome handle(ChatbotContext context, BotInput input) {
        Proposal proposal = context.getProposalContext().getProposal();
        if (!(proposal instanceof MarkOccurrenceProposal markProposal)) {
            return HandlerOutcome.FAILURE;
        }

        // Save the proposal if not already saved (to get an ID for reloading)
        if (markProposal.getId() == null) {
            log.debug("Proposal not yet persisted, saving to database");
            markProposal = proposalRepository.save(markProposal);
            context.getProposalContext().setProposal(markProposal);
        }

        // Reload proposal from database to get the embedding set by async detection
        // The async detection listener may have updated the proposal with embedding
        log.debug("Reloading proposal ID {} from database to get latest embedding", markProposal.getId());
        MarkOccurrenceProposal reloadedProposal = proposalRepository.findById(markProposal.getId())
                .orElse(markProposal);

        // Update the embedding in the context's proposal reference
        if (reloadedProposal.getEmbedding() != null) {
            log.debug("Found embedding in reloaded proposal, length: {}", reloadedProposal.getEmbedding().length);
            markProposal.setEmbedding(reloadedProposal.getEmbedding());
        } else {
            log.warn("Reloaded proposal still has no embedding. Detection may not have completed yet.");
        }

        // suggestMarks will now handle analysis internally if needed
        List<Mark> suggestedMarks = proposalFlowService.suggestMarks(markProposal);
        
        List<String> suggestedMarkIds = suggestedMarks.stream()
                .map(mark -> mark.getId().toString())
                .collect(Collectors.toList());
        context.getProposalContext().setSuggestedMarkIds(suggestedMarkIds);

        log.info("Photo analysis complete for proposal ID: {}. Found {} suggestions.", markProposal.getId(), suggestedMarks.size());

        if (suggestedMarks.isEmpty()) {
            markProposal.setNewMark(true);
            markProposal.setExistingMark(null);
        }

        return HandlerOutcome.SUCCESS;
    }

    @Override
    public ConversationState canHandle() {
        return ProposalState.AWAITING_PHOTO_ANALYSIS;
    }

    @Override
    public boolean isAutomatic() {
        return true;
    }
}
