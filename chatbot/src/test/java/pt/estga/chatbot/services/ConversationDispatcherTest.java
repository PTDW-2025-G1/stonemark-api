package pt.estga.chatbot.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.estga.chatbot.constants.MessageKey;
import pt.estga.chatbot.context.ChatbotContext;
import pt.estga.chatbot.context.ConversationState;
import pt.estga.chatbot.context.ConversationStateHandler;
import pt.estga.chatbot.context.CoreState;
import pt.estga.chatbot.context.HandlerOutcome;
import pt.estga.chatbot.context.SubmissionState;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.models.text.RichText;
import pt.estga.chatbot.services.messages.UiTextService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationDispatcherTest {

    @Mock
    private UiTextService textService;

    private RichText dummyText = new RichText.Plain("dummy");

    private ChatbotContext context;
    private BotInput input;
    private ConversationStateHandler startHandler;
    private ConversationStateHandler mainMenuHandler;

    @BeforeEach
    void setUp() {
        context = new ChatbotContext();
        context.setCurrentState(CoreState.START);

        input = BotInput.builder()
                .userId("123")
                .chatId(456L)
                .platform(Platform.TELEGRAM)
                .type(BotInput.InputType.TEXT)
                .text("/start")
                .build();

        startHandler = mockHandler(CoreState.START);
        mainMenuHandler = mockHandler(CoreState.MAIN_MENU);

        lenient().when(textService.get(any(MessageKey.class))).thenReturn(dummyText);
        lenient().when(textService.get(any(MessageKey.class), any())).thenReturn(dummyText);
        lenient().when(textService.get(any(String.class))).thenReturn(dummyText);
    }

    @Test
    void shouldDispatchToCorrectHandlerAndTransitionState() {
        when(startHandler.handle(context, input)).thenReturn(HandlerOutcome.SUCCESS);
        when(startHandler.getNextState(context, CoreState.START, HandlerOutcome.SUCCESS, input))
                .thenReturn(CoreState.MAIN_MENU);

        List<BotResponse> expected = List.of(BotResponse.builder().build());
        when(mainMenuHandler.createResponse(any(), any(), any())).thenReturn(expected);

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler, mainMenuHandler), textService);

        List<BotResponse> result = dispatcher.dispatch(context, input);

        assertThat(result).isEqualTo(expected);
        assertThat(context.getCurrentState()).isEqualTo(CoreState.MAIN_MENU);
        assertThat(context.getConsecutiveFailures()).isZero();
    }

    @Test
    void shouldRedispatchWhenHandlerReturnsRedispatch() {
        doAnswer(inv -> {
            context.setCurrentState(CoreState.MAIN_MENU);
            return HandlerOutcome.REDISPATCH;
        }).when(startHandler).handle(context, input);

        when(mainMenuHandler.handle(any(), any())).thenReturn(HandlerOutcome.SUCCESS);
        when(mainMenuHandler.getNextState(any(), any(), any(), any())).thenReturn(CoreState.MAIN_MENU);

        List<BotResponse> expected = List.of(BotResponse.builder().build());
        when(mainMenuHandler.createResponse(any(), any(), any())).thenReturn(expected);

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler, mainMenuHandler), textService);

        List<BotResponse> result = dispatcher.dispatch(context, input);

        assertThat(result).isEqualTo(expected);
        verify(mainMenuHandler).handle(any(), eq(input));
    }

    @Test
    void shouldIncrementFailureCountOnFailureAndStayInState() {
        when(startHandler.handle(context, input)).thenReturn(HandlerOutcome.FAILURE);
        when(startHandler.getNextState(context, CoreState.START, HandlerOutcome.FAILURE, input))
                .thenReturn(CoreState.START);
        when(startHandler.createResponse(any(), any(), any())).thenReturn(List.of());

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler), textService);

        dispatcher.dispatch(context, input);

        assertThat(context.getConsecutiveFailures()).isEqualTo(1);
        assertThat(context.getCurrentState()).isEqualTo(CoreState.START);
    }

    @Test
    void shouldResetFailureCountAfterSuccess() {
        context.setConsecutiveFailures(2);

        when(startHandler.handle(context, input)).thenReturn(HandlerOutcome.SUCCESS);
        when(startHandler.getNextState(any(), any(), any(), any())).thenReturn(CoreState.MAIN_MENU);
        when(mainMenuHandler.createResponse(any(), any(), any())).thenReturn(List.of());

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler, mainMenuHandler), textService);

        dispatcher.dispatch(context, input);

        assertThat(context.getConsecutiveFailures()).isZero();
    }

    @Test
    void shouldResetToStartAfterMaxConsecutiveFailures() {
        context.setConsecutiveFailures(3);

        when(startHandler.handle(context, input)).thenReturn(HandlerOutcome.FAILURE);

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler), textService);

        List<BotResponse> result = dispatcher.dispatch(context, input);

        assertThat(context.getCurrentState()).isEqualTo(CoreState.START);
        assertThat(context.getSubmissionContext().getStagedFileId()).isNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUiComponent()).isNotNull();
    }

    @Test
    void shouldReturnErrorResponseWhenMaxDepthExceeded() {
        ConversationStateHandler endlessHandler = mockHandler(CoreState.START);
        when(endlessHandler.handle(any(), any())).thenReturn(HandlerOutcome.REDISPATCH);

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(endlessHandler), textService);

        List<BotResponse> result = dispatcher.dispatch(context, input);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldReturnErrorResponseWhenNoHandlerForState() {
        context.setCurrentState(SubmissionState.AWAITING_LOCATION);

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler), textService);

        List<BotResponse> result = dispatcher.dispatch(context, input);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldExecuteAutomaticHandlersInLoop() {
        ConversationStateHandler autoHandler = mockHandler(SubmissionState.SUBMISSION_STATE);
        when(autoHandler.isAutomatic()).thenReturn(true);
        when(autoHandler.handle(any(), any())).thenReturn(HandlerOutcome.SUCCESS);

        when(startHandler.handle(context, input)).thenReturn(HandlerOutcome.SUCCESS);
        when(startHandler.getNextState(context, CoreState.START, HandlerOutcome.SUCCESS, input))
                .thenReturn(SubmissionState.SUBMISSION_STATE);

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler, autoHandler), textService);

        dispatcher.dispatch(context, input);

        verify(autoHandler).handle(any(), any());
        assertThat(context.getCurrentState()).isEqualTo(SubmissionState.SUBMISSION_STATE);
    }

    @Test
    void shouldStopAutomaticHandlerLoopWhenNotAutomatic() {
        when(startHandler.handle(context, input)).thenReturn(HandlerOutcome.SUCCESS);
        when(startHandler.getNextState(context, CoreState.START, HandlerOutcome.SUCCESS, input))
                .thenReturn(CoreState.MAIN_MENU);
        when(mainMenuHandler.isAutomatic()).thenReturn(false);
        when(mainMenuHandler.createResponse(any(), any(), any())).thenReturn(List.of());

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler, mainMenuHandler), textService);

        dispatcher.dispatch(context, input);

        verify(mainMenuHandler, never()).handle(any(), any());
    }

    @Test
    void shouldReturnEmptyWhenResponseHandlerIsNull() {
        ConversationStateHandler orphanedHandler = mockHandler(CoreState.START);
        when(orphanedHandler.handle(context, input)).thenReturn(HandlerOutcome.SUCCESS);
        when(orphanedHandler.getNextState(any(), any(), any(), any()))
                .thenReturn(SubmissionState.AWAITING_LOCATION);

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(orphanedHandler), textService);

        List<BotResponse> result = dispatcher.dispatch(context, input);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowOnDuplicateHandlerRegistration() {
        ConversationStateHandler h1 = mockHandler(CoreState.START);
        ConversationStateHandler h2 = mockHandler(CoreState.START);

        assertThatThrownBy(() -> new ConversationDispatcher(List.of(h1, h2), textService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate handler");
    }

    @Test
    void shouldThrowWhenHandlerReturnsNullCanHandle() {
        ConversationStateHandler bad = new ConversationStateHandler() {
            @Override public ConversationState canHandle() { return null; }
            @Override public HandlerOutcome handle(ChatbotContext c, BotInput i) { return null; }
            @Override public ConversationState getNextState(ChatbotContext c, ConversationState s, HandlerOutcome o, BotInput i) { return null; }
            @Override public List<BotResponse> createResponse(ChatbotContext c, HandlerOutcome o, BotInput i) { return List.of(); }
        };

        assertThatThrownBy(() -> new ConversationDispatcher(List.of(bad), textService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("null from canHandle");
    }

    @Test
    void shouldRollbackStateOnResponseError() {
        when(startHandler.handle(context, input)).thenReturn(HandlerOutcome.SUCCESS);
        when(startHandler.getNextState(context, CoreState.START, HandlerOutcome.SUCCESS, input))
                .thenReturn(CoreState.MAIN_MENU);
        when(mainMenuHandler.createResponse(any(), any(), any()))
                .thenThrow(new RuntimeException("response failure"));

        ConversationDispatcher dispatcher = new ConversationDispatcher(
                List.of(startHandler, mainMenuHandler), textService);

        assertThatThrownBy(() -> dispatcher.dispatch(context, input))
                .isInstanceOf(RuntimeException.class);

        assertThat(context.getCurrentState()).isEqualTo(CoreState.START);
    }

    private static ConversationStateHandler mockHandler(ConversationState state) {
        ConversationStateHandler handler = org.mockito.Mockito.mock(ConversationStateHandler.class);
        lenient().when(handler.canHandle()).thenReturn(state);
        return handler;
    }
}
