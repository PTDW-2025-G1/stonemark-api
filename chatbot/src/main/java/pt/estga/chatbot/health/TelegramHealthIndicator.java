package pt.estga.chatbot.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("telegram")
public class TelegramHealthIndicator implements HealthIndicator {

    @Value("${telegram.bot.username:}")
    private String botUsername;

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Override
    public Health health() {
        if (botUsername == null || botUsername.isBlank()) {
            return Health.down().withDetail("error", "telegram.bot.username missing").build();
        }
        if (botToken == null || botToken.isBlank()) {
            return Health.down().withDetail("error", "telegram.bot.token missing").build();
        }
        return Health.up().withDetail("username", botUsername).build();
    }
}


