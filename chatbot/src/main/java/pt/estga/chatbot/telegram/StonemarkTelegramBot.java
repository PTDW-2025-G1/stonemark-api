package pt.estga.chatbot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.services.BotEngine;
import pt.estga.chatbot.models.BotInput;
import pt.estga.chatbot.models.BotResponse;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class StonemarkTelegramBot extends TelegramWebhookBot {

    private final String botUsername;
    private final String botPath;
    private final BotEngine conversationService;
    private final TelegramAdapter telegramAdapter;
    private final Executor botExecutor;

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
        // Run asynchronously with executor
        CompletableFuture.runAsync(() -> {
            try {
                BotInput botInput = telegramAdapter.toBotInput(update);
                if (botInput != null) {
                    dispatchAndSend(botInput);
                }
            } catch (Exception e) {
                log.error("Error processing Telegram update", e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }, botExecutor);

        // Telegram webhook can return null since responses are sent asynchronously
        return null;
    }

    // Reusable path for webhook and internal notifications that need dispatcher-driven output.
    public void dispatchAndSend(BotInput botInput) {
        List<BotResponse> botResponses = conversationService.handleInput(botInput);
        if (botResponses == null) {
            return;
        }

        try {
            sendBotResponses(botInput.getChatId(), botResponses);
        } catch (Exception e) {
            log.error("Error dispatching and sending bot responses", e);
        }
    }

    private void sendBotResponses(long chatId, List<BotResponse> botResponses) {
        for (BotResponse response : botResponses) {
            List<PartialBotApiMethod<?>> methods = telegramAdapter.toBotApiMethod(chatId, response);
            if (methods == null) {
                continue;
            }
            for (PartialBotApiMethod<?> method : methods) {
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
