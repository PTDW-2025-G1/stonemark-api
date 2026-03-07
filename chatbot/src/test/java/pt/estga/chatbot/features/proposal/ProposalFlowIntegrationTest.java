package pt.estga.chatbot.features.proposal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProposalFlowIntegrationTest {

    private ProposalFlowStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ProposalFlowStrategy();
    }

    @Test
    void testCompleteLinearFlow_ToMainMenu() {
        ChatbotContext context = new ChatbotContext();

        ConversationState state = strategy.getNextState(context, ProposalState.PROPOSAL_START, HandlerOutcome.SUCCESS);
        assertEquals(ProposalState.WAITING_FOR_PHOTO, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(ProposalState.AWAITING_LOCATION, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(ProposalState.AWAITING_NOTES, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(ProposalState.SUBMITTED, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(CoreState.MAIN_MENU, state);
    }

    @Test
    void testFailure_DoesNotAdvanceState() {
        ChatbotContext context = new ChatbotContext();

        ConversationState state = strategy.getNextState(context, ProposalState.WAITING_FOR_PHOTO, HandlerOutcome.FAILURE);

        assertEquals(ProposalState.WAITING_FOR_PHOTO, state);
    }
}
