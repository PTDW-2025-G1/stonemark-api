package pt.estga.chatbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotInitializer implements CommandLineRunner {

    private final StonemarkTelegramBot telegramBot;

    @Value("${application.base-url}")
    private String baseUrl;

    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    @Override
    public void run(String... args) {
        String webhookUrl = baseUrl + webhookPath;
        log.debug("Setting Telegram webhook (url suppressed)");

        try {
            SetWebhook setWebhook = new SetWebhook();
            setWebhook.setUrl(webhookUrl);
            telegramBot.execute(setWebhook);
            log.debug("Telegram webhook set successfully");
        } catch (TelegramApiException e) {
            log.error("Error setting Telegram webhook: {}", e.getMessage());
        }
    }
}
