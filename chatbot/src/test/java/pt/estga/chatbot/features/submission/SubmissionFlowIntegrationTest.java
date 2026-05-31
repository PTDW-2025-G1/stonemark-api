package pt.estga.chatbot.features.submission;

import org.junit.jupiter.api.Test;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.features.core.MainMenuFactory;
import pt.estga.chatbot.features.submission.handlers.*;
import pt.estga.chatbot.models.BotInput;
import pt.estga.fileapi.FileStorageOperations;
import pt.estga.chatbot.services.UiTextService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SubmissionFlowIntegrationTest {

    private static final BotInput EMPTY_INPUT = null;

    @Test
    void testCompleteLinearFlow_ToMainMenu() {
        var fileStorage = mock(FileStorageOperations.class);
        var textService = mock(UiTextService.class);
        var menuFactory = mock(MainMenuFactory.class);

        ChatbotContext context = new ChatbotContext();

        ConversationState state = new SubmissionStartHandler()
                .getNextState(context, SubmissionState.SUBMISSION_STATE, new HandlerOutcome.Success(), EMPTY_INPUT);
        assertEquals(SubmissionState.WAITING_FOR_PHOTO, state);

        state = new InitialPhotoHandler(fileStorage, textService)
                .getNextState(context, state, new HandlerOutcome.Success(), EMPTY_INPUT);
        assertEquals(SubmissionState.AWAITING_LOCATION, state);

        state = new InitialLocationHandler(textService)
                .getNextState(context, state, new HandlerOutcome.Success(), EMPTY_INPUT);
        assertEquals(SubmissionState.AWAITING_NOTES, state);

        state = new AddNotesHandler(null, textService)
                .getNextState(context, state, new HandlerOutcome.Success(), EMPTY_INPUT);
        assertEquals(SubmissionState.SUBMITTED, state);

        state = new SubmittedHandler(textService, menuFactory)
                .getNextState(context, state, new HandlerOutcome.Success(), EMPTY_INPUT);
        assertEquals(CoreState.MAIN_MENU, state);
    }

    @Test
    void testFailure_DoesNotAdvanceState() {
        InitialPhotoHandler handler = new InitialPhotoHandler(
                mock(FileStorageOperations.class), mock(UiTextService.class));
        ChatbotContext context = new ChatbotContext();

        ConversationState state = handler.getNextState(context, SubmissionState.WAITING_FOR_PHOTO, new HandlerOutcome.Failure(), EMPTY_INPUT);

        assertEquals(SubmissionState.WAITING_FOR_PHOTO, state);
    }
}
