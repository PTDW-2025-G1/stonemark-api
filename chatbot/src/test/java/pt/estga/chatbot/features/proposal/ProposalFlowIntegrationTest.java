package pt.estga.chatbot.features.proposal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.ProposalState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProposalFlowIntegrationTest {

    private ProposalFlowStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ProposalFlowStrategy();
    }

    @Test
    void testMarkSelectionFlow_MultipleMarksFound() {
        ChatbotContext context = new ChatbotContext();

        // Simulate photo analysis finding multiple marks
        context.getProposalContext().setSuggestedMarkIds(List.of("1", "2", "3"));

        // After photo analysis, should go to AWAITING_MARK_SELECTION
        ConversationState nextState = strategy.getNextState(
                context,
                ProposalState.AWAITING_PHOTO_ANALYSIS,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.AWAITING_MARK_SELECTION, nextState,
                "When multiple marks are found, should transition to AWAITING_MARK_SELECTION");
    }

    @Test
    void testMarkSelectionFlow_UserSelectsMark() {
        ChatbotContext context = new ChatbotContext();

        // User selects a mark (SUCCESS outcome)
        ConversationState nextState = strategy.getNextState(
                context,
                ProposalState.AWAITING_MARK_SELECTION,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.MARK_SELECTED, nextState,
                "When user selects a mark, should transition to MARK_SELECTED");
    }

    @Test
    void testMarkSelectionFlow_UserProposesNew() {
        ChatbotContext context = new ChatbotContext();

        // User chooses to propose new mark (PROPOSE_NEW outcome)
        ConversationState nextState = strategy.getNextState(
                context,
                ProposalState.AWAITING_MARK_SELECTION,
                HandlerOutcome.PROPOSE_NEW
        );

        assertEquals(ProposalState.AWAITING_MONUMENT_SUGGESTIONS, nextState,
                "When user proposes new mark, should transition to AWAITING_MONUMENT_SUGGESTIONS");
    }

    @Test
    void testMarkSelectionFlow_SingleMarkFound() {
        ChatbotContext context = new ChatbotContext();

        // Simulate photo analysis finding single mark
        context.getProposalContext().setSuggestedMarkIds(List.of("1"));

        // After photo analysis, should go to WAITING_FOR_MARK_CONFIRMATION
        ConversationState nextState = strategy.getNextState(
                context,
                ProposalState.AWAITING_PHOTO_ANALYSIS,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.WAITING_FOR_MARK_CONFIRMATION, nextState,
                "When single mark is found, should transition to WAITING_FOR_MARK_CONFIRMATION");
    }

    @Test
    void testMarkSelectionFlow_NoMarksFound() {
        ChatbotContext context = new ChatbotContext();

        // Simulate photo analysis finding no marks
        context.getProposalContext().setSuggestedMarkIds(List.of());

        // After photo analysis, should skip mark selection and go to monument suggestions
        ConversationState nextState = strategy.getNextState(
                context,
                ProposalState.AWAITING_PHOTO_ANALYSIS,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.AWAITING_MONUMENT_SUGGESTIONS, nextState,
                "When no marks are found, should transition to AWAITING_MONUMENT_SUGGESTIONS");
    }

    @Test
    void testMarkSelectionFlow_FailureStaysInSameState() {
        ChatbotContext context = new ChatbotContext();

        // Simulate failure outcome
        ConversationState nextState = strategy.getNextState(
                context,
                ProposalState.AWAITING_MARK_SELECTION,
                HandlerOutcome.FAILURE
        );

        assertEquals(ProposalState.AWAITING_MARK_SELECTION, nextState,
                "When handler returns FAILURE, should stay in same state");
    }

    @Test
    void testMarkSelectedToMonumentSuggestions() {
        ChatbotContext context = new ChatbotContext();

        // After mark is selected, should go to monument suggestions
        ConversationState nextState = strategy.getNextState(
                context,
                ProposalState.MARK_SELECTED,
                HandlerOutcome.SUCCESS
        );

        assertEquals(ProposalState.AWAITING_MONUMENT_SUGGESTIONS, nextState,
                "After mark is selected, should transition to AWAITING_MONUMENT_SUGGESTIONS");
    }

    @Test
    void testCompleteFlow_MultipleMarksToCompletion() {
        ChatbotContext context = new ChatbotContext();

        // Step 1: Photo analysis finds multiple marks
        context.getProposalContext().setSuggestedMarkIds(List.of("1", "2", "3"));
        ConversationState state = strategy.getNextState(
                context, ProposalState.AWAITING_PHOTO_ANALYSIS, HandlerOutcome.SUCCESS);
        assertEquals(ProposalState.AWAITING_MARK_SELECTION, state);

        // Step 2: User selects a mark
        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(ProposalState.MARK_SELECTED, state);

        // Step 3: Transition to monument suggestions
        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(ProposalState.AWAITING_MONUMENT_SUGGESTIONS, state);

        // Verify we successfully went through the mark selection flow
        assertNotNull(state);
    }
}

