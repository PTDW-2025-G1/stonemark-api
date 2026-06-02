package pt.estga.chatbot.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;
import pt.estga.chatbot.models.Platform;
import pt.estga.chatbot.services.BotEngine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StonemarkTelegramBotTest {

    private BotEngine botEngine;
    private TelegramAdapter telegramAdapter;
    private Executor botExecutor;
    private ChatLock chatLock;

    @BeforeEach
    void setUp() {
        botEngine = mock(BotEngine.class);
        telegramAdapter = mock(TelegramAdapter.class);
        chatLock = new ChatLock();
        botExecutor = Runnable::run;
    }

    @Test
    void shouldHandleDuplicateUpdatesGracefully() {
        CollectingBot bot = createCollectingBot();
        Update update = createTextUpdate(100L, 1L, "hello");

        bot.onWebhookUpdateReceived(update);
        bot.onWebhookUpdateReceived(update);

        verify(telegramAdapter, atLeastOnce()).toBotInput(update);
    }

    @Test
    void shouldHandleNullUpdate() {
        CollectingBot bot = createCollectingBot();
        bot.onWebhookUpdateReceived(null);
    }

    @Test
    void shouldAckCallbackQueryAndDispatchAsync() {
        CollectingBot bot = createCollectingBot();
        long chatId = 123L;
        Update update = createCallbackUpdate(200L, chatId, "callback_data", "cb_id_123");

        BotInput botInput = BotInput.builder()
                .userId("user_1")
                .chatId(chatId)
                .platform(Platform.TELEGRAM)
                .type(BotInput.InputType.CALLBACK)
                .build();
        when(telegramAdapter.toBotInput(update)).thenReturn(botInput);

        bot.onWebhookUpdateReceived(update);

        List<BotApiMethod<?>> methods = bot.getExecutedMethods();
        assertThat(methods).hasSize(1);
        assertThat(methods.getFirst()).isInstanceOf(AnswerCallbackQuery.class);
        verify(botEngine).handleInput(botInput);
    }

    @Test
    void shouldSkipDispatchWhenAdapterReturnsNull() {
        CollectingBot bot = createCollectingBot();
        Update update = createTextUpdate(300L, 1L, "text");
        when(telegramAdapter.toBotInput(update)).thenReturn(null);

        bot.onWebhookUpdateReceived(update);

        verify(botEngine, never()).handleInput(any());
    }

    @Test
    void shouldSendBotResponsesThroughAdapter() {
        CollectingBot bot = createCollectingBot();
        long chatId = 5L;
        BotInput botInput = BotInput.builder()
                .chatId(chatId)
                .platform(Platform.TELEGRAM)
                .type(BotInput.InputType.TEXT)
                .build();

        BotResponse response = BotResponse.builder().build();
        when(botEngine.handleInput(botInput)).thenReturn(List.of(response));

        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "test");
        when(telegramAdapter.toBotApiMethod(eq(chatId), eq(response)))
                .thenReturn(List.of(sendMessage));

        bot.dispatchAndSend(botInput);

        assertThat(bot.getExecutedMethods()).contains(sendMessage);
    }

    @Test
    void shouldNotSendWhenAdapterReturnsNullMethods() {
        CollectingBot bot = createCollectingBot();
        long chatId = 6L;
        BotInput botInput = BotInput.builder()
                .chatId(chatId)
                .platform(Platform.TELEGRAM)
                .type(BotInput.InputType.TEXT)
                .build();

        BotResponse response = BotResponse.builder().build();
        when(botEngine.handleInput(botInput)).thenReturn(List.of(response));
        when(telegramAdapter.toBotApiMethod(chatId, response)).thenReturn(null);

        bot.dispatchAndSend(botInput);

        assertThat(bot.getExecutedMethods()).isEmpty();
    }

    @Test
    void shouldHandleMultipleResponses() {
        CollectingBot bot = createCollectingBot();
        long chatId = 9L;
        BotInput botInput = BotInput.builder()
                .chatId(chatId)
                .platform(Platform.TELEGRAM)
                .type(BotInput.InputType.TEXT)
                .build();

        BotResponse r1 = BotResponse.builder().build();
        BotResponse r2 = BotResponse.builder().build();
        when(botEngine.handleInput(botInput)).thenReturn(List.of(r1, r2));

        SendMessage msg1 = new SendMessage(String.valueOf(chatId), "first");
        SendMessage msg2 = new SendMessage(String.valueOf(chatId), "second");
        when(telegramAdapter.toBotApiMethod(eq(chatId), any(BotResponse.class)))
                .thenReturn(List.of(msg1))
                .thenReturn(List.of(msg2));

        bot.dispatchAndSend(botInput);

        assertThat(bot.getExecutedMethods()).contains(msg1, msg2);
    }

    @Test
    void shouldRetryOnTransientErrors() {
        AtomicInteger attempts = new AtomicInteger(0);
        StonemarkTelegramBot bot = new FailingBot("testBot", "fake-token", "test-path",
                botEngine, telegramAdapter, botExecutor, chatLock,
                () -> attempts.incrementAndGet() < 2,
                new TelegramApiException("429 Too Many Requests"));

        long chatId = 7L;
        BotInput botInput = BotInput.builder()
                .chatId(chatId).platform(Platform.TELEGRAM).type(BotInput.InputType.TEXT).build();
        BotResponse response = BotResponse.builder().build();
        when(botEngine.handleInput(botInput)).thenReturn(List.of(response));

        SendMessage msg = new SendMessage(String.valueOf(chatId), "test");
        when(telegramAdapter.toBotApiMethod(chatId, response)).thenReturn(List.of(msg));

        bot.dispatchAndSend(botInput);

        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void shouldGiveUpAfterMaxRetries() {
        AtomicInteger attempts = new AtomicInteger(0);
        StonemarkTelegramBot bot = new FailingBot("testBot", "fake-token", "test-path",
                botEngine, telegramAdapter, botExecutor, chatLock,
                () -> { attempts.incrementAndGet(); return true; },
                new TelegramApiException("502 Bad Gateway"));

        long chatId = 8L;
        BotInput botInput = BotInput.builder()
                .chatId(chatId).platform(Platform.TELEGRAM).type(BotInput.InputType.TEXT).build();
        BotResponse response = BotResponse.builder().build();
        when(botEngine.handleInput(botInput)).thenReturn(List.of(response));

        SendMessage msg = new SendMessage(String.valueOf(chatId), "test");
        when(telegramAdapter.toBotApiMethod(chatId, response)).thenReturn(List.of(msg));

        bot.dispatchAndSend(botInput);

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void isRetryableShouldDetectTransientErrors() {
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException("429 Too Many Requests"))).isTrue();
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException("502 Bad Gateway"))).isTrue();
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException("503 Service Unavailable"))).isTrue();
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException("Connection timed out"))).isTrue();
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException("Connection reset"))).isTrue();
    }

    @Test
    void isRetryableShouldRejectPermanentErrors() {
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException("400 Bad Request"))).isFalse();
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException("404 Not Found"))).isFalse();
        assertThat(StonemarkTelegramBot.isRetryable(new TelegramApiException((String) null))).isFalse();
    }

    @Test
    void shouldHandleAdapterExceptionInAsyncProcessing() {
        CollectingBot bot = createCollectingBot();
        Update update = createTextUpdate(400L, 1L, "cause error");
        when(telegramAdapter.toBotInput(update)).thenThrow(new RuntimeException("adapter failure"));

        bot.onWebhookUpdateReceived(update);
    }

    @Test
    void isDuplicateUpdateShouldReturnTrueForRepeatAndFalseForNew() {
        CollectingBot bot = createCollectingBot();

        assertThat(bot.isDuplicateUpdate(1L)).isFalse();
        assertThat(bot.isDuplicateUpdate(1L)).isTrue();
        assertThat(bot.isDuplicateUpdate(2L)).isFalse();
    }

    @Test
    void shouldGetCorrectBotUsernameAndPath() {
        CollectingBot bot = createCollectingBot();
        assertThat(bot.getBotUsername()).isEqualTo("testBot");
        assertThat(bot.getBotPath()).isEqualTo("test-path");
    }

    private CollectingBot createCollectingBot() {
        return new CollectingBot("testBot", "fake-token", "test-path",
                botEngine, telegramAdapter, botExecutor, chatLock);
    }

    private static Update createTextUpdate(long updateId, long chatId, String text) {
        User user = new User();
        user.setId(chatId);

        Message message = new Message();
        message.setFrom(user);
        message.setText(text);

        Update update = new Update();
        update.setUpdateId((int) updateId);
        update.setMessage(message);
        return update;
    }

    private static Update createCallbackUpdate(long updateId, long chatId, String data, String callbackId) {
        User user = new User();
        user.setId(chatId);

        Message message = new Message();

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId(callbackId);
        callbackQuery.setData(data);
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);

        Update update = new Update();
        update.setUpdateId((int) updateId);
        update.setCallbackQuery(callbackQuery);
        return update;
    }

    static class CollectingBot extends StonemarkTelegramBot {
        private final List<BotApiMethod<?>> executedMethods = new ArrayList<>();

        CollectingBot(String botUsername, String botToken, String botPath,
                      BotEngine conversationService, TelegramAdapter telegramAdapter,
                      Executor botExecutor, ChatLock chatLock) {
            super(botUsername, botToken, botPath, conversationService,
                    telegramAdapter, botExecutor, chatLock);
        }

        List<BotApiMethod<?>> getExecutedMethods() {
            return executedMethods;
        }

        @Override
        public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
            if (executedMethods != null) {
                executedMethods.add(method);
            }
            return null;
        }
    }

    static class FailingBot extends StonemarkTelegramBot {
        private final BooleanSupplier shouldFail;
        private final TelegramApiException exception;

        FailingBot(String botUsername, String botToken, String botPath,
                   BotEngine conversationService, TelegramAdapter telegramAdapter,
                   Executor botExecutor, ChatLock chatLock,
                   BooleanSupplier shouldFail, TelegramApiException exception) {
            super(botUsername, botToken, botPath, conversationService,
                    telegramAdapter, botExecutor, chatLock);
            this.shouldFail = shouldFail;
            this.exception = exception;
        }

        @Override
        public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
            if (shouldFail != null && shouldFail.getAsBoolean()) {
                throw exception;
            }
            return null;
        }
    }
}
