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
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class StonemarkTelegramBot extends TelegramWebhookBot {

    private final String botUsername;
    private final String botPath;
    private final BotEngine conversationService;
    private final TelegramAdapter telegramAdapter;
    private final Executor botExecutor;
    // Map used to serialize processing per chatId to prevent concurrent dispatch races
    private final ConcurrentMap<Long, Object> chatLocks = new ConcurrentHashMap<>();

    public StonemarkTelegramBot(String botUsername,
                                String botToken,
                                String botPath,
                                BotEngine conversationService,
                                TelegramAdapter telegramAdapter,
                                Executor botExecutor) {
        super(botToken);
        this.botUsername = botUsername;
        this.botPath = botPath;
        this.conversationService = conversationService;
        this.telegramAdapter = telegramAdapter;
        this.botExecutor = botExecutor;
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
        log.debug("onWebhookUpdateReceived invoked for updateId={}", update == null ? "<null>" : update.getUpdateId());
        try {
            if (update == null) {
                log.warn("Received null Telegram update");
            } else if (update.hasCallbackQuery() && update.getCallbackQuery() != null) {
                CallbackQuery cq = update.getCallbackQuery();
                String callbackId = cq.getId();
                String data = cq.getData();
                Integer messageId = cq.getMessage() != null ? cq.getMessage().getMessageId() : null;
                Long fromId = cq.getFrom() != null ? cq.getFrom().getId() : null;
                log.debug("Received callback query id={} data={} from={} messageId={}", callbackId, data, fromId, messageId);

                // Acknowledge callback right away to stop the Telegram client spinner. Failure to ack
                // can make the client appear to hang even if server-side processing later runs.
                try {
                    AnswerCallbackQuery ack = new AnswerCallbackQuery(callbackId);
                    ack.setText("");
                    execute(ack);
                    log.debug("Sent AnswerCallbackQuery ack for id={}", callbackId);
                } catch (TelegramApiException e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("query is too old")) {
                        // Known Telegram condition: callback query expired on client side; not actionable.
                        log.debug("AnswerCallbackQuery not sent because query is too old for id={}", cq.getId());
                    } else {
                        log.warn("Failed to send AnswerCallbackQuery ack for id={}", cq.getId(), e);
                    }
                }
            } else {
                log.debug("Received update: {}", update.getUpdateId());
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
                    log.debug("Dispatching bot input for chatId={}", botInput.getChatId());
                    dispatchAndSend(botInput);
                } else {
                    log.debug("telegramAdapter.toBotInput returned null for updateId={}", update == null ? "<null>" : update.getUpdateId());
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

    // Reusable path for webhook and internal notifications that need dispatcher-driven output.
    public void dispatchAndSend(BotInput botInput) {
        long chatId = botInput.getChatId();

        // Obtain a per-chat lock object and serialize dispatch for this chat.
        // Create or obtain a per-chat lock object without using a lambda to avoid synthetic
        // parameter warnings from static analysis tools.
        Object lock = chatLocks.get(chatId);
        if (lock == null) {
            Object newLock = new Object();
            Object existing = chatLocks.putIfAbsent(chatId, newLock);
            lock = existing == null ? newLock : existing;
        }

        log.debug("Attempting to acquire chat lock for chatId={}", chatId);
        synchronized (lock) {
            log.debug("Acquired chat lock for chatId={}", chatId);
            List<BotResponse> botResponses = conversationService.handleInput(botInput);
            if (botResponses == null) {
                // Attempt to remove the lock to avoid unbounded map growth.
                chatLocks.remove(chatId, lock);
                return;
            }

            try {
                log.debug("Dispatching {} responses for chatId={}", botResponses.size(), chatId);
                sendBotResponses(chatId, botResponses);
            } catch (Exception e) {
                log.error("Error dispatching and sending bot responses", e);
                throw e;
            } finally {
                log.debug("Releasing chat lock for chatId={}", chatId);
                // Remove lock reference when done to keep map size bounded. Removal is conditional
                // to avoid removing a lock instance that was replaced concurrently.
                chatLocks.remove(chatId, lock);
            }
        }
    }

    private void sendBotResponses(long chatId, List<BotResponse> botResponses) {
        for (BotResponse response : botResponses) {
            List<PartialBotApiMethod<?>> methods = telegramAdapter.toBotApiMethod(chatId, response);
            if (methods == null) {
                continue;
            }
            for (PartialBotApiMethod<?> method : methods) {
                log.debug("Sending Telegram method {} for chatId={}", method == null ? "<null>" : method.getClass().getSimpleName(), chatId);
                try {
                    if (method instanceof BotApiMethod<?>) {
                        execute((BotApiMethod<?>) method);
                    } else if (method instanceof SendPhoto) {
                        execute((SendPhoto) method);
                    }
                } catch (TelegramApiException e) {
                    log.error("Error sending Telegram response", e);
                }
            }
        }
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotPath() { return botPath; }
}
