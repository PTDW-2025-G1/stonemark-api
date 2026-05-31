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

class SubmissionFlowStrategyTest {

    private SubmissionFeature feature;

    @BeforeEach
    void setUp() {
        feature = new SubmissionFeature(mock(UiTextService.class), mock(MainMenuFactory.class));
    }

    @Test
    void getNextState_ShouldAdvanceFromStartToPhoto_WhenSuccess() {
        ConversationState nextState = feature.getNextState(
                new ChatbotContext(),
                SubmissionState.SUBMISSION_STATE,
                new HandlerOutcome.Success()
        );

        assertEquals(SubmissionState.WAITING_FOR_PHOTO, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromPhotoToLocation_WhenSuccess() {
        ConversationState nextState = feature.getNextState(
                new ChatbotContext(),
                SubmissionState.WAITING_FOR_PHOTO,
                new HandlerOutcome.Success()
        );

        assertEquals(SubmissionState.AWAITING_LOCATION, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromLocationToNotes_WhenSuccess() {
        ConversationState nextState = feature.getNextState(
                new ChatbotContext(),
                SubmissionState.AWAITING_LOCATION,
                new HandlerOutcome.Success()
        );

        assertEquals(SubmissionState.AWAITING_NOTES, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromSubmittedToMainMenu_WhenSuccess() {
        ConversationState nextState = feature.getNextState(
                new ChatbotContext(),
                SubmissionState.SUBMITTED,
                new HandlerOutcome.Success()
        );

        assertEquals(CoreState.MAIN_MENU, nextState);
    }

    @Test
    void getNextState_ShouldStayInSameState_WhenFailure() {
        ConversationState nextState = feature.getNextState(
                new ChatbotContext(),
                SubmissionState.AWAITING_LOCATION,
                new HandlerOutcome.Failure()
        );

        assertEquals(SubmissionState.AWAITING_LOCATION, nextState);
    }
}
