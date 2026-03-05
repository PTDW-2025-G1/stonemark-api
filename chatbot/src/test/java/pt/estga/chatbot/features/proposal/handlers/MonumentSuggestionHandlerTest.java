package pt.estga.chatbot.features.proposal.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.content.entities.Monument;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.MonumentProposal;
import pt.estga.proposal.services.chatbot.MarkOccurrenceProposalChatbotFlowService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonumentSuggestionHandlerTest {

    @Mock
    private MarkOccurrenceProposalChatbotFlowService proposalFlowService;

    private MonumentSuggestionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MonumentSuggestionHandler(proposalFlowService);
    }

    @Test
    void canHandle_ShouldReturnAwaitingMonumentSuggestions() {
        assertEquals(ProposalState.AWAITING_MONUMENT_SUGGESTIONS, handler.canHandle());
    }

    @Test
    void isAutomatic_ShouldReturnTrue() {
        assertTrue(handler.isAutomatic());
    }

    @Test
    void handle_ShouldReturnFailure_WhenProposalIsNotMarkOccurrenceProposal() {
        ChatbotContext context = new ChatbotContext();
        context.getProposalContext().setProposal(new MonumentProposal());

        HandlerOutcome outcome = handler.handle(context, BotInput.builder().build());

        assertEquals(HandlerOutcome.FAILURE, outcome);
    }

    @Test
    void handle_ShouldReturnSuccess_WhenNoMonumentsFound() {
        ChatbotContext context = new ChatbotContext();
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        context.getProposalContext().setProposal(proposal);

        when(proposalFlowService.suggestMonuments(any(MarkOccurrenceProposal.class)))
                .thenReturn(Collections.emptyList());

        HandlerOutcome outcome = handler.handle(context, BotInput.builder().build());

        assertEquals(HandlerOutcome.SUCCESS, outcome);
        assertTrue(context.getProposalContext().getSuggestedMonumentIds().isEmpty());
    }

    @Test
    void handle_ShouldReturnSuccess_WhenMonumentsFound() {
        ChatbotContext context = new ChatbotContext();
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        context.getProposalContext().setProposal(proposal);

        Monument monument1 = Monument.builder().id(1L).build();
        Monument monument2 = Monument.builder().id(2L).build();

        when(proposalFlowService.suggestMonuments(any(MarkOccurrenceProposal.class)))
                .thenReturn(List.of(monument1, monument2));

        HandlerOutcome outcome = handler.handle(context, BotInput.builder().build());

        assertEquals(HandlerOutcome.SUCCESS, outcome);
        List<String> suggestedIds = context.getProposalContext().getSuggestedMonumentIds();
        assertEquals(2, suggestedIds.size());
        assertTrue(suggestedIds.contains("1"));
        assertTrue(suggestedIds.contains("2"));
    }
}
