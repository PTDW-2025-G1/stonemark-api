package pt.estga.chatbot.features.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.services.UiTextService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SubmissionFlowIntegrationTest {

    private SubmissionFeature feature;

    @BeforeEach
    void setUp() {
        feature = new SubmissionFeature(mock(UiTextService.class), mock(MainMenuFactory.class));
    }

    @Test
    void testCompleteLinearFlow_ToMainMenu() {
        ChatbotContext context = new ChatbotContext();

        ConversationState state = feature.getNextState(context, SubmissionState.SUBMISSION_STATE, new HandlerOutcome.Success());
        assertEquals(SubmissionState.WAITING_FOR_PHOTO, state);

        state = feature.getNextState(context, state, new HandlerOutcome.Success());
        assertEquals(SubmissionState.AWAITING_LOCATION, state);

        state = feature.getNextState(context, state, new HandlerOutcome.Success());
        assertEquals(SubmissionState.AWAITING_NOTES, state);

        state = feature.getNextState(context, state, new HandlerOutcome.Success());
        assertEquals(SubmissionState.SUBMITTED, state);

        state = feature.getNextState(context, state, new HandlerOutcome.Success());
        assertEquals(CoreState.MAIN_MENU, state);
    }

    @Test
    void testFailure_DoesNotAdvanceState() {
        ChatbotContext context = new ChatbotContext();

        ConversationState state = feature.getNextState(context, SubmissionState.WAITING_FOR_PHOTO, new HandlerOutcome.Failure());

        assertEquals(SubmissionState.WAITING_FOR_PHOTO, state);
    }
}
