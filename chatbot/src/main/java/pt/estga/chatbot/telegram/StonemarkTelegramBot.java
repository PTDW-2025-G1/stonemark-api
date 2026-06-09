package pt.estga.chatbot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.springframework.security.core.Authentication;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.services.BotEngine;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class StonemarkTelegramBot extends TelegramWebhookBot {

    private final String botUsername;
    private final String botPath;
    private final BotEngine conversationService;
    private final TelegramAdapter telegramAdapter;
    private final Executor botExecutor;
    private final ChatLock chatLock;
    // Idempotency guard: tracks recently processed Telegram update IDs to discard duplicate deliveries
    private final ConcurrentHashMap<Long, Long> processedUpdates = new ConcurrentHashMap<>();
    private static final long UPDATE_ID_TTL_MS = 60_000;
    private static final int UPDATE_ID_CACHE_MAX = 10_000;
    // Retry configuration for transient Telegram API errors
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 1_000;

    public StonemarkTelegramBot(String botUsername,
                                String botToken,
                                String botPath,
                                BotEngine conversationService,
                                TelegramAdapter telegramAdapter,
                                Executor botExecutor,
                                ChatLock chatLock) {
        super(botToken);
        this.botUsername = botUsername;
        this.botPath = botPath;
        this.conversationService = conversationService;
        this.telegramAdapter = telegramAdapter;
        this.botExecutor = botExecutor;
        this.chatLock = chatLock;
        setBotCommands();
    }

    private void setBotCommands() {
        List<BotCommand> commands = List.of(
                new BotCommand("start", "Start a new conversation"),
                new BotCommand("options", "Show main options")
        );
        try {
            this.execute(new org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands(commands, null, null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot commands", e);
        }
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        try {
            if (update == null) {
                log.warn("Received null Telegram update");
            } else if (isDuplicateUpdate(update.getUpdateId())) {
            } else if (update.hasCallbackQuery() && update.getCallbackQuery() != null) {
                CallbackQuery cq = update.getCallbackQuery();
                String callbackId = cq.getId();

                try {
                    AnswerCallbackQuery ack = new AnswerCallbackQuery(callbackId);
                    ack.setText("");
                    execute(ack);
                } catch (TelegramApiException e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("query is too old")) {
                    } else {
                        log.warn("Failed to send AnswerCallbackQuery ack for id={}", cq.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during synchronous webhook handling", e);
        }

        // Capture authentication for propagation into async thread to preserve impersonation/audit context if present.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Run asynchronously with executor and restore authentication inside the task.
        CompletableFuture.runAsync(() -> {
            try {
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                BotInput botInput = telegramAdapter.toBotInput(update);
                if (botInput != null) {
                    dispatchAndSend(botInput);
                }
            } catch (Exception e) {
                log.error("Error processing Telegram update asynchronously", e);
            } finally {
                // Ensure SecurityContext is cleared to avoid leaking authentication to other requests/threads.
                SecurityContextHolder.clearContext();
            }
        }, botExecutor);

        // Telegram webhook can return null since responses are sent asynchronously
        return null;
    }

    public void dispatchAndSend(BotInput botInput) {
        long chatId = botInput.getChatId();

        chatLock.lock(chatId);
        try {
            List<BotResponse> botResponses = conversationService.handleInput(botInput);
            if (botResponses != null) {
                sendBotResponses(chatId, botResponses);
            }
        } catch (Exception e) {
            log.error("Error dispatching and sending bot responses", e);
            throw e;
        } finally {
            chatLock.unlock(chatId);
        }
    }

    private void sendBotResponses(long chatId, List<BotResponse> botResponses) {
        for (BotResponse response : botResponses) {
            List<PartialBotApiMethod<?>> methods = telegramAdapter.toBotApiMethod(chatId, response);
            if (methods == null) {
                continue;
            }
            for (PartialBotApiMethod<?> method : methods) {
                executeWithRetry(method, chatId);
            }
        }
    }

    private void executeWithRetry(PartialBotApiMethod<?> method, long chatId) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                if (method instanceof BotApiMethod<?>) {
                    execute((BotApiMethod<?>) method);
                } else if (method instanceof SendPhoto) {
                    execute((SendPhoto) method);
                }
                return;
            } catch (TelegramApiException e) {
                if (!isRetryable(e) || attempt >= MAX_RETRIES) {
                    log.error("Failed to send Telegram response to chatId={} after {} attempt(s): {}", chatId, attempt, e.getMessage());
                    return;
                }
                long delay = RETRY_BASE_DELAY_MS * (1L << (attempt - 1));
                log.warn("Transient error sending to chatId={}, retrying in {}ms (attempt {}/{})", chatId, delay, attempt, MAX_RETRIES);
                try {
                    MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    static boolean isRetryable(TelegramApiException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        // Rate limit, network timeout, or server error — retryable
        if (msg.contains("429") || msg.contains("Too Many Requests")) return true;
        if (msg.contains("502") || msg.contains("Bad Gateway")) return true;
        if (msg.contains("503") || msg.contains("Service Unavailable")) return true;
        return msg.contains("timed out") || msg.contains("timeout") || msg.contains("Connection reset");
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotPath() { return botPath; }

    boolean isDuplicateUpdate(long updateId) {
        Long previous = processedUpdates.putIfAbsent(updateId, System.currentTimeMillis());
        if (previous != null) {
            return true;
        }
        // Evict stale entries when cache grows large to prevent unbounded memory growth
        if (processedUpdates.size() > UPDATE_ID_CACHE_MAX) {
            long cutoff = System.currentTimeMillis() - UPDATE_ID_TTL_MS;
            processedUpdates.values().removeIf(v -> v < cutoff);
        }
        return false;
    }
}
