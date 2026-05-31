package pt.estga.chatbot.features.submission;

import org.junit.jupiter.api.Test;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.models.BotInput;
import pt.estga.fileapi.FileStorageOperations;
import pt.estga.chatbot.services.messages.UiTextService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SubmissionFlowStrategyTest {

    private static final BotInput EMPTY_INPUT = null;

    @Test
    void getNextState_ShouldAdvanceFromSubmissionStartToPhoto_WhenSuccess() {
        SubmissionStartHandler handler = new SubmissionStartHandler();
        ConversationState nextState = handler.getNextState(
                new ChatbotContext(),
                SubmissionState.SUBMISSION_STATE,
                new HandlerOutcome.Success(),
                EMPTY_INPUT
        );
        assertEquals(SubmissionState.WAITING_FOR_PHOTO, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromPhotoToLocation_WhenSuccess() {
        InitialPhotoHandler handler = new InitialPhotoHandler(
                mock(FileStorageOperations.class), mock(UiTextService.class));
        ConversationState nextState = handler.getNextState(
                new ChatbotContext(),
                SubmissionState.WAITING_FOR_PHOTO,
                new HandlerOutcome.Success(),
                EMPTY_INPUT
        );
        assertEquals(SubmissionState.AWAITING_LOCATION, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromLocationToNotes_WhenSuccess() {
        InitialLocationHandler handler = new InitialLocationHandler(mock(UiTextService.class));
        ConversationState nextState = handler.getNextState(
                new ChatbotContext(),
                SubmissionState.AWAITING_LOCATION,
                new HandlerOutcome.Success(),
                EMPTY_INPUT
        );
        assertEquals(SubmissionState.AWAITING_NOTES, nextState);
    }

    @Test
    void getNextState_ShouldAdvanceFromSubmittedToMainMenu_WhenSuccess() {
        SubmittedHandler handler = new SubmittedHandler(mock(UiTextService.class), mock(MainMenuFactory.class));
        ConversationState nextState = handler.getNextState(
                new ChatbotContext(),
                SubmissionState.SUBMITTED,
                new HandlerOutcome.Success(),
                EMPTY_INPUT
        );
        assertEquals(CoreState.MAIN_MENU, nextState);
    }

    @Test
    void getNextState_ShouldStayInSameState_WhenFailure() {
        InitialLocationHandler handler = new InitialLocationHandler(mock(UiTextService.class));
        ConversationState nextState = handler.getNextState(
                new ChatbotContext(),
                SubmissionState.AWAITING_LOCATION,
                new HandlerOutcome.Failure(),
                EMPTY_INPUT
        );
        assertEquals(SubmissionState.AWAITING_LOCATION, nextState);
    }
}
