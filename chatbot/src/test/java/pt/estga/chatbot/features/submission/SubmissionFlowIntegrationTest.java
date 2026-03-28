package pt.estga.chatbot.features.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubmissionFlowIntegrationTest {

    private SubmissionFlowStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SubmissionFlowStrategy();
    }

    @Test
    void testCompleteLinearFlow_ToMainMenu() {
        ChatbotContext context = new ChatbotContext();

        ConversationState state = strategy.getNextState(context, SubmissionState.SUBMISSION_STATE, HandlerOutcome.SUCCESS);
        assertEquals(SubmissionState.WAITING_FOR_PHOTO, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(SubmissionState.AWAITING_LOCATION, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(SubmissionState.AWAITING_NOTES, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(SubmissionState.SUBMITTED, state);

        state = strategy.getNextState(context, state, HandlerOutcome.SUCCESS);
        assertEquals(CoreState.MAIN_MENU, state);
    }

    @Test
    void testFailure_DoesNotAdvanceState() {
        ChatbotContext context = new ChatbotContext();

        ConversationState state = strategy.getNextState(context, SubmissionState.WAITING_FOR_PHOTO, HandlerOutcome.FAILURE);

        assertEquals(SubmissionState.WAITING_FOR_PHOTO, state);
    }
}
