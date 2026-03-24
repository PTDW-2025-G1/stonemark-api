package pt.estga.chatbot.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("whatsapp")
public class WhatsAppHealthIndicator implements HealthIndicator {

    @Value("${whatsapp.bot.token:}")
    private String token;

    @Value("${whatsapp.bot.phone-number-id:}")
    private String phoneNumberId;

    @Override
    public Health health() {
        if (token == null || token.isBlank()) {
            return Health.down().withDetail("error", "whatsapp.bot.token missing").build();
        }
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            return Health.down().withDetail("error", "whatsapp.bot.phone-number-id missing").build();
        }
        return Health.up().withDetail("phoneNumberId", phoneNumberId).build();
    }
}
