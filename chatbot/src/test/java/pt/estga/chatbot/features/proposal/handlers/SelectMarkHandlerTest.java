package pt.estga.chatbot.features.proposal.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;
import pt.estga.chatbot.features.proposal.ProposalCallbackData;
import pt.estga.chatbot.models.BotInput;
import pt.estga.content.entities.Mark;
import pt.estga.content.services.MarkQueryService;
import pt.estga.proposal.entities.MarkOccurrenceProposal;
import pt.estga.proposal.entities.MonumentProposal;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectMarkHandlerTest {

    @Mock
    private MarkQueryService markQueryService;

    private SelectMarkHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SelectMarkHandler(markQueryService);
    }

    @Test
    void canHandle_ShouldReturnAwaitingMarkSelection() {
        assertEquals(ProposalState.AWAITING_MARK_SELECTION, handler.canHandle());
    }

    @Test
    void handle_ShouldReturnAwaitingInput_WhenCallbackDataIsNull() {
        ChatbotContext context = new ChatbotContext();
        BotInput input = BotInput.builder().build();

        HandlerOutcome outcome = handler.handle(context, input);

        assertEquals(HandlerOutcome.AWAITING_INPUT, outcome);
    }

    @Test
    void handle_ShouldReturnFailure_WhenProposalIsNotMarkOccurrenceProposal() {
        ChatbotContext context = new ChatbotContext();
        context.getProposalContext().setProposal(new MonumentProposal());
        BotInput input = BotInput.builder().callbackData("some_data").build();

        HandlerOutcome outcome = handler.handle(context, input);

        assertEquals(HandlerOutcome.FAILURE, outcome);
    }

    @Test
    void handle_ShouldReturnSuccess_WhenMarkIsSelected() {
        ChatbotContext context = new ChatbotContext();
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        proposal.setId(1L);
        context.getProposalContext().setProposal(proposal);

        Mark mark = Mark.builder().id(123L).description("Test Mark").build();
        when(markQueryService.findById(123L)).thenReturn(Optional.of(mark));

        BotInput input = BotInput.builder()
                .callbackData(ProposalCallbackData.SELECT_MARK_PREFIX + "123")
                .build();

        HandlerOutcome outcome = handler.handle(context, input);

        assertEquals(HandlerOutcome.SUCCESS, outcome);
        assertEquals(mark, proposal.getExistingMark());
        assertFalse(proposal.isNewMark());
    }

    @Test
    void handle_ShouldReturnFailure_WhenMarkNotFound() {
        ChatbotContext context = new ChatbotContext();
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        proposal.setId(1L);
        context.getProposalContext().setProposal(proposal);

        when(markQueryService.findById(anyLong())).thenReturn(Optional.empty());

        BotInput input = BotInput.builder()
                .callbackData(ProposalCallbackData.SELECT_MARK_PREFIX + "999")
                .build();

        HandlerOutcome outcome = handler.handle(context, input);

        assertEquals(HandlerOutcome.FAILURE, outcome);
    }

    @Test
    void handle_ShouldReturnFailure_WhenMarkIdIsInvalid() {
        ChatbotContext context = new ChatbotContext();
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        context.getProposalContext().setProposal(proposal);

        BotInput input = BotInput.builder()
                .callbackData(ProposalCallbackData.SELECT_MARK_PREFIX + "invalid")
                .build();

        HandlerOutcome outcome = handler.handle(context, input);

        assertEquals(HandlerOutcome.FAILURE, outcome);
    }

    @Test
    void handle_ShouldReturnProposeNew_WhenProposeNewMarkSelected() {
        ChatbotContext context = new ChatbotContext();
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        proposal.setId(1L);
        context.getProposalContext().setProposal(proposal);

        BotInput input = BotInput.builder()
                .callbackData(ProposalCallbackData.PROPOSE_NEW_MARK)
                .build();

        HandlerOutcome outcome = handler.handle(context, input);

        assertEquals(HandlerOutcome.PROPOSE_NEW, outcome);
        assertTrue(proposal.isNewMark());
        assertNull(proposal.getExistingMark());
    }

    @Test
    void handle_ShouldReturnFailure_WhenCallbackDataDoesNotMatch() {
        ChatbotContext context = new ChatbotContext();
        MarkOccurrenceProposal proposal = new MarkOccurrenceProposal();
        context.getProposalContext().setProposal(proposal);

        BotInput input = BotInput.builder()
                .callbackData("unknown_callback")
                .build();

        HandlerOutcome outcome = handler.handle(context, input);

        assertEquals(HandlerOutcome.FAILURE, outcome);
    }
}

