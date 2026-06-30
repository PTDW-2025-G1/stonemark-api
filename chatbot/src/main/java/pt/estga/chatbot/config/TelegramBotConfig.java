package pt.estga.chatbot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import pt.estga.chatbot.services.BotEngine;
import pt.estga.chatbot.telegram.StonemarkTelegramBot;
import pt.estga.chatbot.telegram.TelegramAdapter;

import java.util.concurrent.Executor;
import org.springframework.util.StringUtils;

/**
 * Configuration class for the Telegram Bot integration.
 * Defines the bot bean and its specific execution context to ensure
 * asynchronous processing does not impact the main application threads.
 */
@Configuration
public class TelegramBotConfig {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    /**
     * Creates a dedicated Executor for Bot tasks to ensure isolation and scalability.
     * Wrapped in DelegatingSecurityContextExecutor to propagate SecurityContext to async threads.
     */
    @Bean(name = "botTaskExecutor")
    public Executor botTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("BotAsync-");
        executor.initialize();
        return new DelegatingSecurityContextExecutor(executor);
    }

    /**
     * Configures the main Telegram Bot bean.
     *
     * @param conversationService Service handling business logic and conversation flow.
     * @param telegramAdapter     Adapter to convert Telegram objects to domain objects.
     * @param botTaskExecutor     Dedicated executor for asynchronous update processing.
     */
    @Bean
    public StonemarkTelegramBot stonemarkTelegramBot(
            BotEngine conversationService,
            TelegramAdapter telegramAdapter,
            @Qualifier("botTaskExecutor") Executor botTaskExecutor) {
        return new StonemarkTelegramBot(botUsername, botToken, webhookPath,
                conversationService, telegramAdapter, botTaskExecutor);
    }

    @PostConstruct
    public void validateProperties() {
        if (!StringUtils.hasText(botUsername) || !StringUtils.hasText(botToken)) {
            throw new IllegalStateException("Missing required Telegram bot configuration: telegram.bot.username and telegram.bot.token must be set");
        }
    }
}
